package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;

public interface ErrorCollector<S> {
    void store(int cursor, SuggestionSupplier<S> suggestions, Object reason);

    default void store(int cursor, Object reason) {
        this.store(cursor, SuggestionSupplier.empty(), reason);
    }

    void finish(int cursor);

    public static class LongestOnly<S> implements ErrorCollector<S> {
        private ErrorCollector.LongestOnly.MutableErrorEntry<S>[] entries = new ErrorCollector.LongestOnly.MutableErrorEntry[16];
        private int nextErrorEntry;
        private int lastCursor = -1;

        private void discardErrorsFromShorterParse(int cursor) {
            if (cursor > this.lastCursor) {
                this.lastCursor = cursor;
                this.nextErrorEntry = 0;
            }
        }

        @Override
        public void finish(int cursor) {
            this.discardErrorsFromShorterParse(cursor);
        }

        @Override
        public void store(int cursor, SuggestionSupplier<S> suggestions, Object reason) {
            this.discardErrorsFromShorterParse(cursor);
            if (cursor == this.lastCursor) {
                this.addErrorEntry(suggestions, reason);
            }
        }

        private void addErrorEntry(SuggestionSupplier<S> suggestions, Object reason) {
            int i = this.entries.length;
            if (this.nextErrorEntry >= i) {
                int j = Util.growByHalf(i, this.nextErrorEntry + 1);
                ErrorCollector.LongestOnly.MutableErrorEntry<S>[] mutableerrorentry = new ErrorCollector.LongestOnly.MutableErrorEntry[j];
                System.arraycopy(this.entries, 0, mutableerrorentry, 0, i);
                this.entries = mutableerrorentry;
            }

            int k = this.nextErrorEntry++;
            ErrorCollector.LongestOnly.MutableErrorEntry<S> mutableerrorentry1 = this.entries[k];
            if (mutableerrorentry1 == null) {
                mutableerrorentry1 = new ErrorCollector.LongestOnly.MutableErrorEntry<>();
                this.entries[k] = mutableerrorentry1;
            }

            mutableerrorentry1.suggestions = suggestions;
            mutableerrorentry1.reason = reason;
        }

        public List<ErrorEntry<S>> entries() {
            int i = this.nextErrorEntry;
            if (i == 0) {
                return List.of();
            } else {
                List<ErrorEntry<S>> list = new ArrayList<>(i);

                for (int j = 0; j < i; j++) {
                    ErrorCollector.LongestOnly.MutableErrorEntry<S> mutableerrorentry = this.entries[j];
                    list.add(new ErrorEntry<>(this.lastCursor, mutableerrorentry.suggestions, mutableerrorentry.reason));
                }

                return list;
            }
        }

        public int cursor() {
            return this.lastCursor;
        }

        static class MutableErrorEntry<S> {
            SuggestionSupplier<S> suggestions = SuggestionSupplier.empty();
            Object reason = "empty";
        }
    }

    public static class Nop<S> implements ErrorCollector<S> {
        @Override
        public void store(int p_409770_, SuggestionSupplier<S> p_409587_, Object p_410314_) {
        }

        @Override
        public void finish(int p_409839_) {
        }
    }
}
