package net.goui.cosmicdungeon.client.screen;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.network.LeaderboardPayloads.*;
import net.goui.cosmicdungeon.leaderboard.LeaderboardMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
/** Native focusable widgets, independent list scrolling, and server-selected ranking pages. */
public final class LeaderboardScreen extends Screen{
    private static int sequence;
    private final Screen parent;
    private View view;
    private final List<Button> metrics=new ArrayList<>(),players=new ArrayList<>();
    private final Deque<Cursor> history=new ArrayDeque<>();
    private record Cursor(long value,String id){}
    private Cursor cursor=new Cursor(-1,"");
    private Button metricPrev,metricNext,rankPrev,rankNext,refresh;
    private String metric=LeaderboardMetrics.DEFAULT,metricLabel="Dungeons completed",selected="",status="Reading server statistics...";
    private int metricScroll,playerScroll,requestId,leftWidth,rightX,rowsVisible;
    private boolean pending,queued;
    private long sent;
    public LeaderboardScreen(Screen parent){super(Component.literal("Leaderboard"));this.parent=parent;}
    public static void receive(View payload){
        var mc=Minecraft.getInstance();
        if(mc.player!=null&&mc.screen instanceof LeaderboardScreen screen&&payload.request()==screen.requestId){
            screen.view=payload;screen.pending=false;screen.metric=payload.page().metric();
            screen.status=payload.page().status();screen.updateRows();
        }
    }
    public static int visibleRows(int height){return Math.max(3,Math.min(12,(height-142)/18));}
    @Override protected void init(){
        leftWidth=(width-36)/2;rightX=24+leftWidth;rowsVisible=visibleRows(height);
        metrics.clear();players.clear();
        for(int i=0;i<rowsVisible;i++){
            final int row=i;
            metrics.add(addRenderableWidget(Button.builder(Component.empty(),b->{
                if(view==null)return;int index=metricScroll+row;if(index>=view.metrics().size())return;
                metric=view.metrics().get(index).key();metricLabel=view.metrics().get(index).label();cursor=new Cursor(-1,"");history.clear();playerScroll=0;selected="";queue();
            }).bounds(12,64+i*18,leftWidth,17).build()));
            players.add(addRenderableWidget(new LeaderboardRowButton(rightX,64+i*18,width-12-rightX,b->{
                if(view==null)return;int index=playerScroll+row;if(index<view.rows().size()){selected=view.rows().get(index).id();updateRows();}
            })));
        }
        int nav=height-72;
        metricPrev=addRenderableWidget(Button.builder(Component.literal("<"),b->{metricScroll-=rowsVisible;updateRows();}).bounds(12,nav,30,20).build());
        metricNext=addRenderableWidget(Button.builder(Component.literal(">"),b->{metricScroll+=rowsVisible;updateRows();}).bounds(12+leftWidth-30,nav,30,20).build());
        rankPrev=addRenderableWidget(Button.builder(Component.literal("<"),b->{cursor=history.removeLast();playerScroll=0;queue();}).bounds(rightX,nav,30,20).build());
        rankNext=addRenderableWidget(Button.builder(Component.literal(">"),b->{
            if(view==null||view.rows().isEmpty())return;
            history.addLast(cursor);if(history.size()>256)history.removeFirst();
            var last=view.rows().getLast();cursor=new Cursor(last.value(),last.id());playerScroll=0;queue();
        }).bounds(width-42,nav,30,20).build());
        refresh=addRenderableWidget(Button.builder(Component.literal("Refresh"),b->queue()).bounds(width/2-84,height-24,80,20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2+4,height-24,80,20).build());
        if(view==null)queue();else updateRows();
    }
    private void queue(){queued=true;status="Reading server statistics...";updateRows();}
    @Override public void tick(){
        long now=System.nanoTime();
        if(pending&&now-sent>15_000_000_000L){pending=false;status="Read timed out. Refresh to retry.";updateRows();}
        if(queued&&!pending&&now-sent>=300_000_000L){
            queued=false;pending=true;sent=now;requestId=sequence=sequence==Integer.MAX_VALUE?0:sequence+1;
            ModNetwork.sendToServer(new Request(requestId,metric,"",0,cursor.value(),cursor.id()));updateRows();
        }
    }
    private void updateRows(){
        if(refresh==null)return;
        boolean enabled=!pending&&!queued;
        metricScroll=Math.max(0,Math.min(metricScroll,view==null?0:Math.max(0,view.metrics().size()-rowsVisible)));
        playerScroll=Math.max(0,Math.min(playerScroll,view==null?0:Math.max(0,view.rows().size()-rowsVisible)));
        for(int i=0;i<rowsVisible;i++){
            var button=metrics.get(i);int index=i+metricScroll;
            button.visible=view!=null&&index<view.metrics().size();button.active=enabled;
            if(button.visible){
                var row=view.metrics().get(index);boolean chosen=row.key().equals(metric);
                button.setMessage(Component.literal((chosen?"> ":"")+font.plainSubstrByWidth(row.label(),leftWidth-16))
                    .withColor(chosen?0x66E2CF:0xFFFFFF));
                button.setTooltip(Tooltip.create(Component.literal(row.label()+"\n"+row.key())));
            }
            button=players.get(i);index=i+playerScroll;
            button.visible=view!=null&&index<view.rows().size();button.active=enabled;
            if(button.visible){
                var row=view.rows().get(index);String score=LeaderboardMetrics.format(metric,row.value());
                ((LeaderboardRowButton)button).row((view.page().firstRank()+index)+". "+row.name(),score,row.id().equals(selected));
                button.setTooltip(Tooltip.create(Component.literal(row.name()+"\n"+score+" ("+row.value()+")\n"+row.id())));
            }
        }
        metricPrev.active=enabled&&metricScroll>0;metricNext.active=enabled&&view!=null&&metricScroll+rowsVisible<view.metrics().size();
        metricPrev.visible=metricNext.visible=view!=null&&view.metrics().size()>rowsVisible;
        rankPrev.active=enabled&&!history.isEmpty();rankNext.active=enabled&&view!=null&&view.page().more();
        refresh.active=enabled;
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(y>=64&&y<64+rowsVisible*18&&view!=null){
            int delta=vertical>0?-1:vertical<0?1:0;
            if(x>=12&&x<12+leftWidth)metricScroll+=delta;
            else if(x>=rightX&&x<width-12)playerScroll+=delta;else return super.mouseScrolled(x,y,horizontal,vertical);
            updateRows();return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        super.render(g,x,y,partial);
        g.drawCenteredString(font,title,width/2,12,0xFF66E2CF);
        String label=metricLabel;
        if(view!=null)for(var choice:view.metrics())if(choice.key().equals(metric)){label=choice.label();break;}
        g.drawString(font,font.plainSubstrByWidth(label,width-rightX-12),rightX,40,0xFFCCF5ED,false);
        g.drawCenteredString(font,"Statistics",12+leftWidth/2,40,0xFFCCF5ED);
        g.drawCenteredString(font,"Rankings",rightX+(width-12-rightX)/2,height-66,0xFFE4E4E4);
        g.drawCenteredString(font,font.plainSubstrByWidth(status,width-24),width/2,height-44,0xFFB9CBC8);
        if(y>=height-46&&y<height-30)g.setComponentTooltipForNextFrame(font,List.of(Component.literal(status)),x,y);
        if(view!=null&&view.rows().isEmpty()&&!pending&&!queued)g.drawString(font,"No recorded rows",rightX+6,70,0xFFB9CBC8,false);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
