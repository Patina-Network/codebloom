package org.patinanetwork.codebloom.shared.topic;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;
import org.patinanetwork.codebloom.shared.TypableObject;

public final class TopicMetadataList {
    private TopicMetadataList() {}

    public static final Map<LeetcodeTopicEnum, TopicMetadataObject> ENUM_TO_TOPIC_METADATA =
            Collections.unmodifiableMap(generate());

    private static Map<LeetcodeTopicEnum, TopicMetadataObject> generate() {
        return Arrays.stream(LeetcodeTopicEnum.values())
                .collect(Collectors.toMap(
                        topic -> topic, TopicMetadataList::buildMetadata, (a, b) -> a, LinkedHashMap::new));
    }

    private static TopicMetadataObject buildMetadata(LeetcodeTopicEnum topic) {
        return switch (topic) {
            case STACK -> TopicMetadataObject.builder().name("Stack").build();
            case DATA_STREAM ->
                TopicMetadataObject.builder().name("Data Stream").build();
            case REJECTION_SAMPLING ->
                TopicMetadataObject.builder().name("Rejection Sampling").build();
            case GEOMETRY -> TopicMetadataObject.builder().name("Geometry").build();
            case COUNTING -> TopicMetadataObject.builder().name("Counting").build();
            case DESIGN -> TopicMetadataObject.builder().name("Design").build();
            case PROBABILITY_AND_STATISTICS ->
                TopicMetadataObject.builder().name("Probability and Statistics").build();
            case MINIMUM_SPANNING_TREE ->
                TopicMetadataObject.builder()
                        .name("Minimum Spanning Tree")
                        .aliases(List.of("MST"))
                        .build();
            case LINE_SWEEP -> TopicMetadataObject.builder().name("Line Sweep").build();
            case NUMBER_THEORY ->
                TopicMetadataObject.builder().name("Number Theory").build();
            case ROLLING_HASH ->
                TopicMetadataObject.builder().name("Rolling Hash").build();
            case SEGMENT_TREE ->
                TopicMetadataObject.builder().name("Segment Tree").build();
            case BICONNECTED_COMPONENT ->
                TopicMetadataObject.builder().name("Biconnected Component").build();
            case MONOTONIC_STACK ->
                TopicMetadataObject.builder().name("Monotonic Stack").build();
            case ITERATOR -> TopicMetadataObject.builder().name("Iterator").build();
            case QUEUE -> TopicMetadataObject.builder().name("Queue").build();
            case RADIX_SORT -> TopicMetadataObject.builder().name("Radix Sort").build();
            case BUCKET_SORT ->
                TopicMetadataObject.builder().name("Bucket Sort").build();
            case SHELL -> TopicMetadataObject.builder().name("Shell Sort").build();
            case MEMOIZATION ->
                TopicMetadataObject.builder().name("Memoization").build();
            case STRING -> TopicMetadataObject.builder().name("String").build();
            case PREFIX_SUM -> TopicMetadataObject.builder().name("Prefix Sum").build();
            case CONCURRENCY ->
                TopicMetadataObject.builder().name("Concurrency").build();
            case DATABASE ->
                TopicMetadataObject.builder()
                        .name("Database")
                        .aliases(List.of("DB"))
                        .build();
            case SHORTEST_PATH ->
                TopicMetadataObject.builder().name("Shortest Path").build();
            case SORTING -> TopicMetadataObject.builder().name("Sorting").build();
            case LINKED_LIST ->
                TopicMetadataObject.builder().name("Linked List").build();
            case SLIDING_WINDOW ->
                TopicMetadataObject.builder().name("Sliding Window").build();
            case SUFFIX_ARRAY ->
                TopicMetadataObject.builder().name("Suffix Array").build();
            case DOUBLY_LINKED_LIST ->
                TopicMetadataObject.builder().name("Doubly Linked List").build();
            case SIMULATION -> TopicMetadataObject.builder().name("Simulation").build();
            case ORDERED_SET ->
                TopicMetadataObject.builder().name("Ordered Set").build();
            case GRAPH -> TopicMetadataObject.builder().name("Graph").build();
            case MATH -> TopicMetadataObject.builder().name("Math").build();
            case ORDERED_MAP ->
                TopicMetadataObject.builder().name("Ordered Map").build();
            case GAME_THEORY ->
                TopicMetadataObject.builder().name("Game Theory").build();
            case DYNAMIC_PROGRAMMING ->
                TopicMetadataObject.builder()
                        .name("Dynamic Programming")
                        .aliases(List.of("DP"))
                        .build();
            case RECURSION -> TopicMetadataObject.builder().name("Recursion").build();
            case MONOTONIC_QUEUE ->
                TopicMetadataObject.builder().name("Monotonic Queue").build();
            case MATRIX -> TopicMetadataObject.builder().name("Matrix").build();
            case RESERVOIR_SAMPLING ->
                TopicMetadataObject.builder().name("Reservoir Sampling").build();
            case MERGE_SORT -> TopicMetadataObject.builder().name("Merge Sort").build();
            case COMBINATORICS ->
                TopicMetadataObject.builder().name("Combinatorics").build();
            case INTERACTIVE ->
                TopicMetadataObject.builder().name("Interactive").build();
            case BINARY_TREE ->
                TopicMetadataObject.builder().name("Binary Tree").build();
            case RANDOMIZED -> TopicMetadataObject.builder().name("Randomized").build();
            case BITMASK -> TopicMetadataObject.builder().name("Bitmask").build();
            case BREADTH_FIRST_SEARCH ->
                TopicMetadataObject.builder()
                        .name("Breadth-First Search")
                        .aliases(List.of("BFS"))
                        .build();
            case STRING_MATCHING ->
                TopicMetadataObject.builder().name("String Matching").build();
            case GREEDY -> TopicMetadataObject.builder().name("Greedy").build();
            case BRAINTEASER ->
                TopicMetadataObject.builder().name("Brainteaser").build();
            case BACKTRACKING ->
                TopicMetadataObject.builder().name("Backtracking").build();
            case BIT_MANIPULATION ->
                TopicMetadataObject.builder().name("Bit Manipulation").build();
            case UNION_FIND -> TopicMetadataObject.builder().name("Union-Find").build();
            case BINARY_SEARCH_TREE ->
                TopicMetadataObject.builder()
                        .name("Binary Search Tree")
                        .aliases(List.of("BST"))
                        .build();
            case TWO_POINTERS ->
                TopicMetadataObject.builder().name("Two Pointers").build();
            case ARRAY -> TopicMetadataObject.builder().name("Array").build();
            case DEPTH_FIRST_SEARCH ->
                TopicMetadataObject.builder()
                        .name("Depth-First Search")
                        .aliases(List.of("DFS"))
                        .build();
            case EULERIAN_CIRCUIT ->
                TopicMetadataObject.builder().name("Eulerian Circuit").build();
            case TREE -> TopicMetadataObject.builder().name("Tree").build();
            case BINARY_SEARCH ->
                TopicMetadataObject.builder()
                        .name("Binary Search")
                        .aliases(List.of("BS"))
                        .build();
            case STRONGLY_CONNECTED_COMPONENT ->
                TopicMetadataObject.builder()
                        .name("Strongly Connected Component")
                        .build();
            case ENUMERATION ->
                TopicMetadataObject.builder().name("Enumeration").build();
            case HEAP_PRIORITY_QUEUE ->
                TopicMetadataObject.builder().name("Heap / Priority Queue").build();
            case DIVIDE_AND_CONQUER ->
                TopicMetadataObject.builder().name("Divide and Conquer").build();
            case HASH_FUNCTION ->
                TopicMetadataObject.builder().name("Hash Function").build();
            case HASH_TABLE -> TopicMetadataObject.builder().name("Hash Table").build();
            case TRIE -> TopicMetadataObject.builder().name("Trie").build();
            case TOPOLOGICAL_SORT ->
                TopicMetadataObject.builder().name("Topological Sort").build();
            case QUICKSELECT ->
                TopicMetadataObject.builder().name("Quickselect").build();
            case BINARY_INDEXED_TREE ->
                TopicMetadataObject.builder().name("Binary Indexed Tree").build();
            case COUNTING_SORT ->
                TopicMetadataObject.builder().name("Counting Sort").build();
            case UNKNOWN -> TopicMetadataObject.builder().name("Unknown").build();
        };
    }

    @Getter
    @Builder
    public static class TopicMetadataObject implements TypableObject {
        private static final String TS_TYPE = """
                export type TopicMetadataObject = {
                  name: string;
                  aliases?: string[];
                };

                """.stripIndent();

        private final String name;
        private final List<String> aliases;

        @Override
        public String tsType() {
            return TS_TYPE;
        }
    }
}
