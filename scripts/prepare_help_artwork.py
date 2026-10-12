"""Verify or regenerate runtime help PNGs from the committed16:9 JPEG masters.

Build-time maintenance only. Requires Pillow; never imported by the game or CI.
Run without arguments to check, or --write for an intentional asset regeneration.
The Java asset tests remain the dependency-free CI coverage/budget gate.
"""
from pathlib import Path
import argparse
import json
from PIL import Image

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--write', action='store_true')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
assets = root / 'src/main/resources/assets/cosmicdungeon'
manifest = json.loads((assets / 'help_artwork.json').read_text(encoding='utf-8'))
size = manifest['width'], manifest['height']
expected_paths = {assets / theme['texture'] for theme in manifest['themes'].values()}
actual_paths = set((assets / 'textures/gui/help').iterdir())
assert actual_paths == expected_paths, 'Unexpected or missing help asset files: ' + str(actual_paths ^ expected_paths)
total = 0
for name, theme in manifest['themes'].items():
    path = assets / theme['texture']
    with Image.open(root / 'art/help/masters' / (name + '.jpg')) as master:
        assert master.width * 9 == master.height * 16, name
        expected = master.convert('RGB').resize(size, Image.Resampling.LANCZOS)
        if args.write:
            temp = path.with_suffix('.partial')
            expected.save(temp, format='PNG', optimize=True)
            temp.replace(path)
        with Image.open(path) as actual:
            assert actual.size == size and actual.mode == 'RGB', name
            assert actual.tobytes() == expected.tobytes(), 'Unexpected runtime pixels: ' + name
        total += path.stat().st_size
decoded = len(manifest['themes']) * size[0] * size[1] * 4
assert total <= manifest['budgets']['png_bytes'], total
assert decoded <= manifest['budgets']['decoded_rgba_bytes'], decoded
print(json.dumps(dict(themes=len(manifest['themes']), runtime_png_bytes=total,
                     decoded_rgba_bytes=decoded, action='write' if args.write else 'verify')))
