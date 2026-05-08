package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import org.apache.commons.lang3.exception.ExceptionUtils;

class ReportGameListener implements GameTestListener {
    private int attempts = 0;
    private int successes = 0;

    public ReportGameListener() {
    }

    @Override
    public void testStructureLoaded(GameTestInfo testInfo) {
        this.attempts++;
    }

    private void handleRetry(GameTestInfo testInfo, GameTestRunner runner, boolean passed) {
        RetryOptions retryoptions = testInfo.retryOptions();
        String s = String.format(Locale.ROOT, "[Run: %4d, Ok: %4d, Fail: %4d", this.attempts, this.successes, this.attempts - this.successes);
        if (!retryoptions.unlimitedTries()) {
            s = s + String.format(Locale.ROOT, ", Left: %4d", retryoptions.numberOfTries() - this.attempts);
        }

        s = s + "]";
        String s1 = testInfo.id() + " " + (passed ? "passed" : "failed") + "! " + testInfo.getRunTime() + "ms";
        String s2 = String.format(Locale.ROOT, "%-53s%s", s, s1);
        if (passed) {
            reportPassed(testInfo, s2);
        } else {
            say(testInfo.getLevel(), ChatFormatting.RED, s2);
        }

        if (retryoptions.hasTriesLeft(this.attempts, this.successes)) {
            runner.rerunTest(testInfo);
        }
    }

    @Override
    public void testPassed(GameTestInfo test, GameTestRunner runner) {
        this.successes++;
        if (test.retryOptions().hasRetries()) {
            this.handleRetry(test, runner, true);
        } else if (!test.isFlaky()) {
            reportPassed(test, test.id() + " passed! (" + test.getRunTime() + "ms)");
        } else {
            if (this.successes >= test.requiredSuccesses()) {
                reportPassed(test, test + " passed " + this.successes + " times of " + this.attempts + " attempts.");
            } else {
                say(
                    test.getLevel(),
                    ChatFormatting.GREEN,
                    "Flaky test " + test + " succeeded, attempt: " + this.attempts + " successes: " + this.successes
                );
                runner.rerunTest(test);
            }
        }
    }

    @Override
    public void testFailed(GameTestInfo test, GameTestRunner runner) {
        if (!test.isFlaky()) {
            reportFailure(test, test.getError());
            if (test.retryOptions().hasRetries()) {
                this.handleRetry(test, runner, false);
            }
        } else {
            GameTestInstance gametestinstance = test.getTest();
            String s = "Flaky test " + test + " failed, attempt: " + this.attempts + "/" + gametestinstance.maxAttempts();
            if (gametestinstance.requiredSuccesses() > 1) {
                s = s + ", successes: " + this.successes + " (" + gametestinstance.requiredSuccesses() + " required)";
            }

            say(test.getLevel(), ChatFormatting.YELLOW, s);
            if (test.maxAttempts() - this.attempts + this.successes >= test.requiredSuccesses()) {
                runner.rerunTest(test);
            } else {
                reportFailure(test, new ExhaustedAttemptsException(this.attempts, this.successes, test));
            }
        }
    }

    @Override
    public void testAddedForRerun(GameTestInfo oldTest, GameTestInfo newTest, GameTestRunner runner) {
        newTest.addListener(this);
    }

    public static void reportPassed(GameTestInfo testInfo, String message) {
        getTestInstanceBlockEntity(testInfo).ifPresent(p_396406_ -> p_396406_.setSuccess());
        visualizePassedTest(testInfo, message);
    }

    private static void visualizePassedTest(GameTestInfo testInfo, String message) {
        say(testInfo.getLevel(), ChatFormatting.GREEN, message);
        GlobalTestReporter.onTestSuccess(testInfo);
    }

    protected static void reportFailure(GameTestInfo testInfo, Throwable error) {
        Component component;
        if (error instanceof GameTestAssertException gametestassertexception) {
            component = gametestassertexception.getDescription();
        } else {
            component = Component.literal(Util.describeError(error));
        }

        getTestInstanceBlockEntity(testInfo).ifPresent(p_396408_ -> p_396408_.setErrorMessage(component));
        visualizeFailedTest(testInfo, error);
    }

    protected static void visualizeFailedTest(GameTestInfo testInfo, Throwable error) {
        String s = error.getMessage() + (error.getCause() == null ? "" : " cause: " + Util.describeError(error.getCause()));
        String s1 = (testInfo.isRequired() ? "" : "(optional) ") + testInfo.id() + " failed! " + s;
        say(testInfo.getLevel(), testInfo.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, s1);
        Throwable throwable = MoreObjects.firstNonNull(ExceptionUtils.getRootCause(error), error);
        if (throwable instanceof GameTestAssertPosException gametestassertposexception) {
            testInfo.getTestInstanceBlockEntity().markError(gametestassertposexception.getAbsolutePos(), gametestassertposexception.getMessageToShowAtBlock());
        }

        GlobalTestReporter.onTestFailed(testInfo);
    }

    private static Optional<TestInstanceBlockEntity> getTestInstanceBlockEntity(GameTestInfo testInfo) {
        ServerLevel serverlevel = testInfo.getLevel();
        Optional<BlockPos> optional = Optional.ofNullable(testInfo.getTestBlockPos());
        return optional.flatMap(p_396405_ -> serverlevel.getBlockEntity(p_396405_, BlockEntityType.TEST_INSTANCE_BLOCK));
    }

    protected static void say(ServerLevel serverLevel, ChatFormatting formatting, String message) {
        serverLevel.getPlayers(p_177705_ -> true).forEach(p_177709_ -> p_177709_.sendSystemMessage(Component.literal(message).withStyle(formatting)));
    }
}
