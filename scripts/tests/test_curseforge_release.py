import importlib.util
import io
import http.client
import urllib.error
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

spec = importlib.util.spec_from_file_location('publisher', Path(__file__).resolve().parents[1] / 'curseforge_release.py')
p = importlib.util.module_from_spec(spec)
spec.loader.exec_module(p)


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        (self.root / 'build/libs').mkdir(parents=True)
        (self.root / 'docs/releases').mkdir(parents=True)
        (self.root / 'gradle.properties').write_text('mod_version=1.5.2-beta.1\ncurseforgeProjectId=1326805\nminecraft_version=1.21.10\n')
        (self.root / 'docs/releases/1.5.2-beta.1.md').write_text('Public beta.')
        with zipfile.ZipFile(self.root / 'build/libs/cosmicdungeon-1.5.2-beta.1.jar', 'w') as jar:
            jar.writestr('META-INF/neoforge.mods.toml', '[[mods]]\nmodId="cosmicdungeon"\nversion="1.5.2-beta.1"')
        with zipfile.ZipFile(self.root / 'build/libs/cosmicdungeon-1.5.2-beta.1-loading-screen.jar', 'w') as jar:
            jar.writestr(p.SERVICE, 'net.goui.cosmicdungeon.loading.CosmicLoadingWindow\n')
            jar.writestr('META-INF/MANIFEST.MF', 'FMLModType: LIBRARY\nImplementation-Version: 1.5.2-beta.1\n')
            for asset in ['theme-cosmicdungeon.json', 'cd_minecraft.png', 'cd_loading_background.png',
                          'cd_progress_bar_bg.png', 'cd_progress_bar_fg.png']:
                jar.writestr('cosmic-loading/' + asset, 'fixture')
        self.entries = [dict(id=i, name=n, gameVersionTypeID=1) for i, n in
                        enumerate(['1.21.10', 'NeoForge', 'Java 21', 'Client', 'Server'], 1)]

    def test_channels_and_invalid_versions(self):
        for version, kind in [('1.5.2-alpha.1', 'alpha'), ('1.5.2-beta.1', 'beta'), ('1.5.2', 'release')]:
            self.assertEqual(p.channel(version), kind)
        for bad in ['1.5.2-beta', '1.5.02', '1.5.2-beta.0', '1.5.2/other']:
            with self.assertRaises(ValueError):
                p.channel(bad)

    def test_patch_bump_and_stable_promotion(self):
        p.bump('alpha', self.root)
        self.assertEqual(p.properties(self.root)['mod_version'], '1.5.3-alpha.1')
        with self.assertRaises(ValueError):
            p.bump('release', self.root)
        p.bump('beta', self.root)
        p.bump('release', self.root)
        self.assertEqual(p.properties(self.root)['mod_version'], '1.5.4')

    def test_actual_jar_metadata_is_checked(self):
        self.assertEqual(p.validate('v1.5.2-beta.1', self.root)['release_type'], 'beta')
        with self.assertRaises(ValueError):
            p.validate('v1.5.3-beta.1', self.root)
        path = self.root / 'build/libs/cosmicdungeon-1.5.2-beta.1.jar'
        with zipfile.ZipFile(path, 'w') as jar:
            jar.writestr('META-INF/neoforge.mods.toml', '[[mods]]\nmodId="cosmicdungeon"\nversion="1.5.1"')
        with self.assertRaises(ValueError):
            p.validate('v1.5.2-beta.1', self.root)

    def test_game_version_namespace_and_client_only(self):
        # Real collision: the generic type-1 ID is invalid for this mod project.
        entries = self.entries[1:] + [
            dict(id=13966, name='1.21.10', gameVersionTypeID=1),
            dict(id=13964, name='1.21.10', gameVersionTypeID=77784),
            dict(id=16126, name='1.21.10', gameVersionTypeID=615)]
        self.assertEqual(p.game_version_names(entries, '1.21.10', True),
                         ['1.21.10', 'NeoForge', 'Java 21', 'Client'])
        self.assertEqual(p.game_version_names(entries, '1.21.10'),
                         ['1.21.10', 'NeoForge', 'Java 21', 'Client', 'Server'])
        with self.assertRaises(ValueError):
            p.game_version_names(entries, '99.99.99')

    @patch.dict(os.environ, {'CURSEFORGE_API_TOKEN': 'test-only'})
    @patch.object(p, 'git', return_value='commit')
    def test_successful_retry_does_not_upload_again(self, _git):
        plan = p.validate('v1.5.2-beta.1', self.root)
        receipt = self.root / 'receipt.json'
        with patch.object(p, 'request', side_effect=[self.entries, {'id': 100}, {'id': 101}]) as call:
            p.publish(plan, receipt, 0)
            self.assertEqual(call.call_count, 3)
            archive = call.call_args_list[2].args[2].split(b'\r\n\r\n', 1)[1].split(b'\r\n', 1)[0]
            metadata = json.loads(archive)
            self.assertEqual(metadata['parentFileID'], 100)
            self.assertNotIn('gameVersions', metadata)
            self.assertNotIn('gameVersionNames', metadata)
        with patch.object(p, 'request', return_value=self.entries) as call:
            p.publish(plan, receipt, 0)
            self.assertEqual(call.call_count, 1)

    @patch.dict(os.environ, {'CURSEFORGE_API_TOKEN': 'test-only'})
    @patch.object(p, 'git', return_value='commit')
    def test_uncertain_upload_is_not_retried(self, _git):
        plan = p.validate('v1.5.2-beta.1', self.root)
        receipt = self.root / 'receipt.json'
        with patch.object(p, 'request', side_effect=[self.entries, RuntimeError('timeout')]):
            with self.assertRaises(RuntimeError):
                p.publish(plan, receipt, 0)
        self.assertEqual(json.loads(receipt.read_text())['pending']['role'], 'main')
        with patch.object(p, 'request') as call:
            with self.assertRaises(ValueError):
                p.publish(plan, receipt, 0)
            call.assert_not_called()

    @patch.dict(os.environ, {'CURSEFORGE_API_TOKEN': 'test-only'})
    @patch.object(p, 'git', return_value='commit')
    def test_companion_precedes_main_without_redundant_archive(self, _git):
        plan = p.validate('v1.5.2-beta.1', self.root)
        with patch.object(p, 'multipart', return_value=(b'jar', 'multipart/test')) as multipart:
            with patch.object(p, 'request', side_effect=[self.entries, {'id': 90}, {'id': 100}]):
                p.publish(plan, self.root / 'receipt.json', 999, loading_slug='cosmic-loading-screen')
        metadata = [call.args[0] for call in multipart.call_args_list]
        self.assertEqual(metadata[0]['gameVersionNames'], ['1.21.10', 'NeoForge', 'Java 21', 'Client'])
        self.assertEqual(metadata[1]['gameVersionNames'], ['1.21.10', 'NeoForge', 'Java 21', 'Client', 'Server'])
        self.assertNotIn('gameVersions', metadata[0])
        self.assertNotIn('gameVersions', metadata[1])
        self.assertEqual(metadata[1]['relations']['projects'][0]['slug'], 'cosmic-loading-screen')
        self.assertEqual(len(metadata), 2)
        self.assertIsInstance(metadata[1]['relations']['projects'][0]['projectID'], int)
        self.assertEqual(metadata[1]['relations']['projects'][0]['projectID'], 999)


    @patch.dict(os.environ, {'CURSEFORGE_API_TOKEN': 'test-only'})
    @patch.object(p, 'git', return_value='commit')
    def test_enabling_companion_after_legacy_main_keeps_archive(self, _git):
        plan = p.validate('v1.5.2-beta.1', self.root)
        receipt = self.root / 'receipt.json'
        receipt.write_text(json.dumps({
            'version': plan['version'], 'commit': 'commit',
            'sha256': {role: item['sha256'] for role, item in plan['artifacts'].items()},
            'main_project': plan['project_id'],
            'files': {'main': {'project_id': plan['project_id'], 'file_id': 100}}}))
        with patch.object(p, 'multipart', return_value=(b'jar', 'multipart/test')) as multipart:
            with patch.object(p, 'request', side_effect=[self.entries, {'id': 90}, {'id': 101}]):
                result = p.publish(plan, receipt, 999, loading_slug='cosmic-loading-screen')
        self.assertEqual(result['files']['main']['file_id'], 100)
        self.assertEqual(result['files']['loading_archive']['file_id'], 101)
        self.assertEqual(multipart.call_count, 2)
        self.assertEqual(multipart.call_args_list[1].args[0]['parentFileID'], 100)
        self.assertNotIn('main_loading_project', result)

    def test_api_rejection_reports_reason_without_token(self):
        token = 'secret-do-not-log'
        body = json.dumps({'errorCode': 1009,
                           'errorMessage': 'Invalid game version ID 13966. ' + token,
                           'untrusted': 'do not print arbitrary response fields'}).encode()
        error = urllib.error.HTTPError('https://example.invalid', 400, 'Bad Request', {}, io.BytesIO(body))
        with patch.object(p.urllib.request, 'urlopen', side_effect=error):
            with self.assertRaises(RuntimeError) as caught:
                p.request('/projects/1326805/upload-file', token, b'fixture', 'multipart/test')
        message = str(caught.exception)
        self.assertIn('1009', message)
        self.assertIn('13966', message)
        self.assertNotIn(token, message)
        self.assertNotIn('untrusted', message)

    def test_html_api_error_body_is_not_logged(self):
        error = urllib.error.HTTPError('https://example.invalid', 502, 'Bad Gateway', {},
                                       io.BytesIO(b'<html>private server diagnostics</html>'))
        with patch.object(p.urllib.request, 'urlopen', side_effect=error):
            with self.assertRaises(RuntimeError) as caught:
                p.request('/game/versions', 'secret')
        self.assertIn('502', str(caught.exception))
        self.assertNotIn('private server diagnostics', str(caught.exception))


    def test_truncated_api_error_body_stays_bounded(self):
        error = urllib.error.HTTPError('https://example.invalid', 502, 'Bad Gateway', {}, io.BytesIO())
        with patch.object(error, 'read', side_effect=http.client.IncompleteRead(b'private diagnostic', 50)):
            with patch.object(p.urllib.request, 'urlopen', side_effect=error):
                with self.assertRaises(RuntimeError) as caught:
                    p.request('/game/versions', 'secret')
        self.assertIn('502', str(caught.exception))
        self.assertNotIn('private diagnostic', str(caught.exception))


if __name__ == '__main__':
    unittest.main()
