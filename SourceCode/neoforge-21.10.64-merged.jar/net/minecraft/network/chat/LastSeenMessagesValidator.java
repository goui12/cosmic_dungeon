package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import javax.annotation.Nullable;

public class LastSeenMessagesValidator {
    private final int lastSeenCount;
    private final ObjectList<LastSeenTrackedEntry> trackedMessages = new ObjectArrayList<>();
    @Nullable
    private MessageSignature lastPendingMessage;

    public LastSeenMessagesValidator(int lastSeenCount) {
        this.lastSeenCount = lastSeenCount;

        for (int i = 0; i < lastSeenCount; i++) {
            this.trackedMessages.add(null);
        }
    }

    public void addPending(MessageSignature signature) {
        if (!signature.equals(this.lastPendingMessage)) {
            this.trackedMessages.add(new LastSeenTrackedEntry(signature, true));
            this.lastPendingMessage = signature;
        }
    }

    public int trackedMessagesCount() {
        return this.trackedMessages.size();
    }

    public void applyOffset(int offset) throws LastSeenMessagesValidator.ValidationException {
        int i = this.trackedMessages.size() - this.lastSeenCount;
        if (offset >= 0 && offset <= i) {
            this.trackedMessages.removeElements(0, offset);
        } else {
            throw new LastSeenMessagesValidator.ValidationException("Advanced last seen window by " + offset + " messages, but expected at most " + i);
        }
    }

    public LastSeenMessages applyUpdate(LastSeenMessages.Update update) throws LastSeenMessagesValidator.ValidationException {
        this.applyOffset(update.offset());
        ObjectList<MessageSignature> objectlist = new ObjectArrayList<>(update.acknowledged().cardinality());
        if (update.acknowledged().length() > this.lastSeenCount) {
            throw new LastSeenMessagesValidator.ValidationException(
                "Last seen update contained " + update.acknowledged().length() + " messages, but maximum window size is " + this.lastSeenCount
            );
        } else {
            for (int i = 0; i < this.lastSeenCount; i++) {
                boolean flag = update.acknowledged().get(i);
                LastSeenTrackedEntry lastseentrackedentry = this.trackedMessages.get(i);
                if (flag) {
                    if (lastseentrackedentry == null) {
                        throw new LastSeenMessagesValidator.ValidationException(
                            "Last seen update acknowledged unknown or previously ignored message at index " + i
                        );
                    }

                    this.trackedMessages.set(i, lastseentrackedentry.acknowledge());
                    objectlist.add(lastseentrackedentry.signature());
                } else {
                    if (lastseentrackedentry != null && !lastseentrackedentry.pending()) {
                        throw new LastSeenMessagesValidator.ValidationException(
                            "Last seen update ignored previously acknowledged message at index " + i + " and signature " + lastseentrackedentry.signature()
                        );
                    }

                    this.trackedMessages.set(i, null);
                }
            }

            LastSeenMessages lastseenmessages = new LastSeenMessages(objectlist);
            if (!update.verifyChecksum(lastseenmessages)) {
                throw new LastSeenMessagesValidator.ValidationException("Checksum mismatch on last seen update: the client and server must have desynced");
            } else {
                return lastseenmessages;
            }
        }
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}
