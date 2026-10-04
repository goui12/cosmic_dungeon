package net.goui.cosmicdungeon.leaderboard;
import net.goui.cosmicdungeon.network.LeaderboardPayloads.Row;
import java.util.*;
/** Streaming cursor pagination retains one page, regardless of the number of historical players. */
public final class LeaderboardRanking{
    public static final Comparator<Row> ORDER=Comparator.comparingLong(Row::value).reversed().thenComparing(Row::id);
    private final PriorityQueue<Row> best=new PriorityQueue<>(ORDER.reversed());
    private final Row cursor;
    private final int limit;
    private long before,eligible;
    public LeaderboardRanking(long after,String id,int limit){
        if(limit<1||limit>100)throw new IllegalArgumentException("Invalid page size");
        this.limit=limit;cursor=after<0?null:new Row(id,"",after);
    }
    public void accept(Row row){
        if(cursor!=null&&ORDER.compare(row,cursor)<=0){before++;return;}
        eligible++;best.add(row);if(best.size()>limit)best.remove();
    }
    public List<Row> rows(){return best.stream().sorted(ORDER).toList();}
    public boolean more(){return eligible>limit;}
    public long firstRank(){return before+1;}
}
