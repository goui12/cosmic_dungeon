package net.goui.cosmicdungeon.economy;

import net.minecraft.nbt.*;
import java.util.*;

/** Immutable first-observation inbox in the same native account image as each balance decision.
 * Chat is a retryable projection, never an acknowledgment. Full decisions stay in the existing ledger. */
public final class WealthReviewState {
    public static final int NOTE_LIMIT=512;
    public record Notice(UUID owner,long threshold,boolean finalReview,String origin,CompoundTag evidence,
                         long sequence,long revision,boolean acknowledged,boolean resolved,
                         String action,String actor,String note,long expectedRevision,long decisionTime) {
        public Notice {
            Objects.requireNonNull(owner);evidence=evidence.copy();EconomyLedger.validateRow(evidence);
            if(threshold<1||sequence<1||revision<0||!owner.toString().equals(evidence.getStringOr("player",""))
                    ||!Set.of("crossing","legacy_balance","legacy_marker","legacy_final").contains(origin)
                    ||!evidence.getCompoundOrEmpty("details").isEmpty()||resolved&&!acknowledged)
                throw new IllegalArgumentException("Invalid wealth review evidence");
            if(origin.equals("crossing")&&(!evidence.getStringOr("status","").equals(AccountTransfer.COMMITTED)
                    ||evidence.getLongOr("before",0)>=threshold||evidence.getLongOr("after",0)<threshold))
                throw new IllegalArgumentException("Wealth crossing does not cross its threshold");
            if(revision==0){
                if(!action.isEmpty()||!actor.isEmpty()||!note.isEmpty()||expectedRevision!=-1||decisionTime!=0||acknowledged||resolved)
                    throw new IllegalArgumentException("Invalid initial review decision");
            }else{
                validateActor(actor);
                if(!Set.of("ack","resolve","reopen").contains(action)||expectedRevision!=revision-1||decisionTime<0
                        ||note.length()>NOTE_LIMIT||(!action.equals("ack")&&note.isBlank())
                        ||action.equals("ack")&&(!acknowledged||resolved||!note.isEmpty())
                        ||action.equals("resolve")&&(!resolved||!acknowledged)
                        ||action.equals("reopen")&&(resolved||acknowledged))
                    throw new IllegalArgumentException("Invalid wealth review decision");
            }
        }
        @Override public CompoundTag evidence(){return evidence.copy();}
        public String key(){return key(owner,threshold);}
        private static String key(UUID owner,long threshold){return owner+"|"+threshold;}
        CompoundTag save(){
            var n=new CompoundTag();n.putString("owner",owner.toString());n.putLong("threshold",threshold);
            n.putBoolean("final",finalReview);n.putString("origin",origin);n.put("evidence",evidence.copy());
            n.putLong("sequence",sequence);n.putLong("revision",revision);n.putBoolean("acknowledged",acknowledged);
            n.putBoolean("resolved",resolved);n.putString("action",action);n.putString("actor",actor);
            n.putString("note",note);n.putLong("expected_revision",expectedRevision);n.putLong("decision_time",decisionTime);return n;
        }
        static Notice load(CompoundTag n){
            for(String key:List.of("threshold","sequence","revision","expected_revision","decision_time"))
                if(!wholeNumber(n.get(key)))throw new IllegalArgumentException("Missing wealth review number "+key);
            for(String key:List.of("final","acknowledged","resolved"))
                if(!(n.get(key) instanceof ByteTag b)||(b.byteValue()!=0&&b.byteValue()!=1))throw new IllegalArgumentException("Invalid review flag");
            for(String key:List.of("owner","origin","action","actor","note"))
                if(!(n.get(key) instanceof StringTag))throw new IllegalArgumentException("Missing wealth review text");
            if(!(n.get("evidence") instanceof CompoundTag))throw new IllegalArgumentException("Missing wealth evidence");
            String owner=n.getStringOr("owner","");UUID id=UUID.fromString(owner);
            if(!id.toString().equals(owner))throw new IllegalArgumentException("Noncanonical review owner");
            return new Notice(id,n.getLongOr("threshold",0),n.getBooleanOr("final",false),n.getStringOr("origin",""),n.getCompoundOrEmpty("evidence"),
                    n.getLongOr("sequence",0),n.getLongOr("revision",-1),n.getBooleanOr("acknowledged",false),n.getBooleanOr("resolved",false),
                    n.getStringOr("action",""),n.getStringOr("actor",""),n.getStringOr("note",""),n.getLongOr("expected_revision",-2),n.getLongOr("decision_time",-1));
        }
    }
    private final long sequence;
    private final Map<String,Notice> entries;
    private final NavigableMap<Long,Notice> pending;
    private final Map<UUID,NavigableMap<Long,Notice>> owners;
    public WealthReviewState(){this(0,Map.of());}
    private WealthReviewState(long sequence,Map<String,Notice> entries){
        this.sequence=sequence;this.entries=Map.copyOf(entries);pending=new TreeMap<>();owners=new HashMap<>();
        var seen=new HashSet<Long>();
        entries.forEach((key,n)->{
            if(!key.equals(n.key())||n.sequence()>sequence||!seen.add(n.sequence()))throw new IllegalArgumentException("Invalid review index");
            if(!n.acknowledged())pending.put(n.sequence(),n);
            owners.computeIfAbsent(n.owner(),ignored->new TreeMap<>()).put(n.threshold(),n);
        });
    }
    public static void validateActor(String actor){
        if(actor.equals("console"))return;
        if(!actor.startsWith("developer:"))throw new IllegalArgumentException("Untrusted review actor");
        String id=actor.substring(10);if(!UUID.fromString(id).toString().equals(id))throw new IllegalArgumentException("Invalid developer UUID");
    }
    private static boolean wholeNumber(Tag value){
        // JsonOps round trips narrow integral NBT types; reject floating-point truncation.
        return value instanceof NumericTag&&!(value instanceof FloatTag)&&!(value instanceof DoubleTag);
    }
    public static WealthReviewState load(CompoundTag root){
        if(root.isEmpty())return new WealthReviewState(); // Optional field in pre-Batch-31 saves.
        if(!wholeNumber(root.get("schema"))||root.getIntOr("schema",0)!=1
                ||!wholeNumber(root.get("sequence"))||root.getLongOr("sequence",-1)<0
                ||!(root.get("entries") instanceof CompoundTag rows))throw new IllegalArgumentException("Wealth review schema requires recovery");
        var entries=new HashMap<String,Notice>();
        for(String key:rows.keySet()){
            if(!(rows.get(key) instanceof CompoundTag row))throw new IllegalArgumentException("Invalid wealth review row");
            entries.put(key,Notice.load(row));
        }
        return new WealthReviewState(root.getLongOr("sequence",0),entries);
    }
    public CompoundTag save(){
        var root=new CompoundTag();root.putInt("schema",1);root.putLong("sequence",sequence);
        var rows=new CompoundTag();entries.forEach((k,n)->rows.put(k,n.save()));root.put("entries",rows);return root;
    }
    public Optional<Notice> find(UUID owner,long threshold){return Optional.ofNullable(entries.get(Notice.key(owner,threshold)));}
    public List<Notice> owner(UUID owner,long afterThreshold,int limit){
        if(limit<1||limit>128||afterThreshold<0)throw new IllegalArgumentException("Invalid review page");
        return owners.getOrDefault(owner,Collections.emptyNavigableMap()).tailMap(afterThreshold,false).values().stream().limit(limit).toList();
    }
    public List<Notice> pending(long afterSequence,int limit){
        if(limit<1||limit>128||afterSequence<0)throw new IllegalArgumentException("Invalid review page");
        return pending.tailMap(afterSequence,false).values().stream().limit(limit).toList();
    }
    public int pendingCount(){return pending.size();}
    public long openFinalCount(){return entries.values().stream().filter(n->n.finalReview()&&!n.resolved()).count();}
    public long openFinalCount(long currentMaximum){
        return entries.values().stream().filter(n->(n.finalReview()||n.threshold()==currentMaximum)&&!n.resolved()).count();
    }
    public WealthReviewState ensure(UUID owner,long threshold,boolean finalReview,String origin,CompoundTag evidence){
        var existing=find(owner,threshold);
        if(existing.isPresent())return this; // First evidence and operator disposition never overwritten by later observations.
        var compact=evidence.copy();compact.put("details",new CompoundTag());
        var n=new Notice(owner,threshold,finalReview,origin,compact,Math.addExact(sequence,1),0,false,false,"","","",-1,0);
        var next=new HashMap<>(entries);next.put(n.key(),n);return new WealthReviewState(n.sequence(),next);
    }
    public WealthReviewState capture(CompoundTag row,long[] thresholds,long maximum){
        if(!row.getStringOr("status","").equals(AccountTransfer.COMMITTED)||row.getLongOr("after",0)<=row.getLongOr("before",0))return this;
        var next=this;UUID owner=UUID.fromString(row.getStringOr("player",""));
        for(long threshold:thresholds)if(row.getLongOr("before",0)<threshold&&row.getLongOr("after",0)>=threshold)
            next=next.ensure(owner,threshold,threshold==maximum,"crossing",row);
        return next;
    }
    public WealthReviewState decide(UUID owner,long threshold,long expected,String action,String actor,String note,long now){
        validateActor(actor);Objects.requireNonNull(note);
        if(expected<0||now<0||!Set.of("ack","resolve","reopen").contains(action)||note.length()>NOTE_LIMIT
                ||action.equals("ack")&&!note.isEmpty()||!action.equals("ack")&&note.isBlank())throw new IllegalArgumentException("Invalid review decision");
        var old=find(owner,threshold).orElseThrow(()->new IllegalArgumentException("No review for this owner and threshold"));
        if(old.expectedRevision()==expected&&old.action().equals(action)&&old.actor().equals(actor)&&old.note().equals(note))return this;
        if(old.revision()!=expected)throw new IllegalStateException("Stale review revision; inspect it again");
        if(action.equals("ack")&&old.acknowledged()||action.equals("resolve")&&old.resolved()||action.equals("reopen")&&!old.resolved())
            throw new IllegalStateException("Review is already in that state");
        long nextSequence=action.equals("reopen")?Math.addExact(sequence,1):sequence;
        var n=new Notice(owner,threshold,old.finalReview(),old.origin(),old.evidence(),action.equals("reopen")?nextSequence:old.sequence(),
                Math.addExact(old.revision(),1),!action.equals("reopen"),action.equals("resolve"),action,actor,note,expected,now);
        var next=new HashMap<>(entries);next.put(n.key(),n);return new WealthReviewState(nextSequence,next);
    }
    // TODO(M02, licensed TEST): Economy Internal 17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28
    // (modified 2026-08-18) requires first threshold notifications and a final economic review.
    // Verify offline developers, reconnect/restart, failed account writes and competing reviewers.
    // Delivery may repeat until explicit acknowledgment; review must never freeze legitimate debits.
}
