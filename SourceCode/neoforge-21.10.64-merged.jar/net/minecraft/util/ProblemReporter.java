package net.minecraft.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public interface ProblemReporter {
    ProblemReporter DISCARDING = new ProblemReporter() {
        @Override
        public ProblemReporter forChild(ProblemReporter.PathElement p_422462_) {
            return this;
        }

        @Override
        public void report(ProblemReporter.Problem p_422437_) {
        }
    };

    ProblemReporter forChild(ProblemReporter.PathElement child);

    void report(ProblemReporter.Problem problem);

    public static class Collector implements ProblemReporter {
        public static final ProblemReporter.PathElement EMPTY_ROOT = () -> "";
        @Nullable
        private final ProblemReporter.Collector parent;
        private final ProblemReporter.PathElement element;
        private final Set<ProblemReporter.Collector.Entry> problems;

        public Collector() {
            this(EMPTY_ROOT);
        }

        public Collector(ProblemReporter.PathElement element) {
            this.parent = null;
            this.problems = new LinkedHashSet<>();
            this.element = element;
        }

        private Collector(ProblemReporter.Collector parent, ProblemReporter.PathElement element) {
            this.problems = parent.problems;
            this.parent = parent;
            this.element = element;
        }

        @Override
        public ProblemReporter forChild(ProblemReporter.PathElement child) {
            return new ProblemReporter.Collector(this, child);
        }

        @Override
        public void report(ProblemReporter.Problem problem) {
            this.problems.add(new ProblemReporter.Collector.Entry(this, problem));
        }

        public boolean isEmpty() {
            return this.problems.isEmpty();
        }

        public void forEach(BiConsumer<String, ProblemReporter.Problem> action) {
            List<ProblemReporter.PathElement> list = new ArrayList<>();
            StringBuilder stringbuilder = new StringBuilder();

            for (ProblemReporter.Collector.Entry problemreporter$collector$entry : this.problems) {
                for (ProblemReporter.Collector problemreporter$collector = problemreporter$collector$entry.source;
                    problemreporter$collector != null;
                    problemreporter$collector = problemreporter$collector.parent
                ) {
                    list.add(problemreporter$collector.element);
                }

                for (int i = list.size() - 1; i >= 0; i--) {
                    stringbuilder.append(list.get(i).get());
                }

                action.accept(stringbuilder.toString(), problemreporter$collector$entry.problem());
                stringbuilder.setLength(0);
                list.clear();
            }
        }

        public String getReport() {
            Multimap<String, ProblemReporter.Problem> multimap = HashMultimap.create();
            this.forEach(multimap::put);
            return multimap.asMap()
                .entrySet()
                .stream()
                .map(
                    p_421345_ -> " at "
                        + p_421345_.getKey()
                        + ": "
                        + p_421345_.getValue().stream().map(ProblemReporter.Problem::description).collect(Collectors.joining("; "))
                )
                .collect(Collectors.joining("\n"));
        }

        public String getTreeReport() {
            List<ProblemReporter.PathElement> list = new ArrayList<>();
            ProblemReporter.Collector.ProblemTreeNode problemreporter$collector$problemtreenode = new ProblemReporter.Collector.ProblemTreeNode(this.element);

            for (ProblemReporter.Collector.Entry problemreporter$collector$entry : this.problems) {
                for (ProblemReporter.Collector problemreporter$collector = problemreporter$collector$entry.source;
                    problemreporter$collector != this;
                    problemreporter$collector = problemreporter$collector.parent
                ) {
                    list.add(problemreporter$collector.element);
                }

                ProblemReporter.Collector.ProblemTreeNode problemreporter$collector$problemtreenode1 = problemreporter$collector$problemtreenode;

                for (int i = list.size() - 1; i >= 0; i--) {
                    problemreporter$collector$problemtreenode1 = problemreporter$collector$problemtreenode1.child(list.get(i));
                }

                list.clear();
                problemreporter$collector$problemtreenode1.problems.add(problemreporter$collector$entry.problem);
            }

            return String.join("\n", problemreporter$collector$problemtreenode.getLines());
        }

        record Entry(ProblemReporter.Collector source, ProblemReporter.Problem problem) {
        }

        record ProblemTreeNode(
            ProblemReporter.PathElement element,
            List<ProblemReporter.Problem> problems,
            Map<ProblemReporter.PathElement, ProblemReporter.Collector.ProblemTreeNode> children
        ) {
            public ProblemTreeNode(ProblemReporter.PathElement p_422382_) {
                this(p_422382_, new ArrayList<>(), new LinkedHashMap<>());
            }

            public ProblemReporter.Collector.ProblemTreeNode child(ProblemReporter.PathElement element) {
                return this.children.computeIfAbsent(element, ProblemReporter.Collector.ProblemTreeNode::new);
            }

            public List<String> getLines() {
                int i = this.problems.size();
                int j = this.children.size();
                if (i == 0 && j == 0) {
                    return List.of();
                } else if (i == 0 && j == 1) {
                    List<String> list1 = new ArrayList<>();
                    this.children.forEach((p_421768_, p_421762_) -> list1.addAll(p_421762_.getLines()));
                    list1.set(0, this.element.get() + list1.get(0));
                    return list1;
                } else if (i == 1 && j == 0) {
                    return List.of(this.element.get() + ": " + this.problems.getFirst().description());
                } else {
                    List<String> list = new ArrayList<>();
                    this.children.forEach((p_422150_, p_422334_) -> list.addAll(p_422334_.getLines()));
                    list.replaceAll(p_422662_ -> "  " + p_422662_);

                    for (ProblemReporter.Problem problemreporter$problem : this.problems) {
                        list.add("  " + problemreporter$problem.description());
                    }

                    list.addFirst(this.element.get() + ":");
                    return list;
                }
            }
        }
    }

    public record ElementReferencePathElement(ResourceKey<?> id) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return "->{" + this.id.location() + "@" + this.id.registry() + "}";
        }
    }

    public record FieldPathElement(String name) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return "." + this.name;
        }
    }

    public record IndexedFieldPathElement(String name, int index) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return "." + this.name + "[" + this.index + "]";
        }
    }

    public record IndexedPathElement(int index) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return "[" + this.index + "]";
        }
    }

    @FunctionalInterface
    public interface PathElement {
        String get();
    }

    public interface Problem {
        String description();
    }

    public record RootElementPathElement(ResourceKey<?> id) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return "{" + this.id.location() + "@" + this.id.registry() + "}";
        }
    }

    public record RootFieldPathElement(String name) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return this.name;
        }
    }

    public static class ScopedCollector extends ProblemReporter.Collector implements AutoCloseable {
        private final Logger logger;

        public ScopedCollector(Logger logger) {
            this.logger = logger;
        }

        public ScopedCollector(ProblemReporter.PathElement element, Logger logger) {
            super(element);
            this.logger = logger;
        }

        @Override
        public void close() {
            if (!this.isEmpty()) {
                this.logger.warn("[{}] Serialization errors:\n{}", this.logger.getName(), this.getTreeReport());
            }
        }
    }
}
