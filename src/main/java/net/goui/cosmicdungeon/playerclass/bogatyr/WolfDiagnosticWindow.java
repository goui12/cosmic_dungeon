package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import java.util.function.Supplier;

/** Fixed-size event accounting; sample construction stops when that event's slots are full. */
final class WolfDiagnosticWindow {
    static final int SAMPLE_LIMIT=2, CONTEXT_LIMIT=16;
    enum Event { JOIN_FRESH, JOIN_DISK, ADDED_FRESH, ADDED_DISK, ADDED_UNKNOWN, UNCONFIRMED_CALLBACK, EGG, BIRTH, MERCENARY_SUMMON,
        REJECT_RUN, REJECT_IDENTITY, REJECT_MERCENARY, REMOVED }
    private final long[] counts=new long[Event.values().length];
    private final EnumMap<Event,List<String>> samples=new EnumMap<>(Event.class);
    private final Map<String,Long> contexts=new LinkedHashMap<>();
    private long overflow;
    void record(Event event,String context,Supplier<String> sample){
        counts[event.ordinal()]++;
        if(contexts.containsKey(context)||contexts.size()<CONTEXT_LIMIT)contexts.merge(context,1L,Long::sum);
        else overflow++;
        var list=samples.computeIfAbsent(event,k->new ArrayList<>());
        if(list.size()<SAMPLE_LIMIT)list.add(sample.get());
    }
    long count(Event event){return counts[event.ordinal()];}
    int samples(){return samples.values().stream().mapToInt(List::size).sum();}
    int contexts(){return contexts.size();}
    long overflow(){return overflow;}
    boolean empty(){return Arrays.stream(counts).allMatch(n->n==0);}
    String summary(){
        var totals=new LinkedHashMap<String,Long>();
        for(var event:Event.values())if(count(event)>0)totals.put(event.name(),count(event));
        return "events="+totals+" contexts="+contexts+" overflowContextEvents="+overflow+" samples="+samples;
    }
    void clear(){Arrays.fill(counts,0);samples.clear();contexts.clear();overflow=0;}
}
