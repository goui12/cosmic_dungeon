package net.goui.cosmicdungeon.client.screen.requests;

import java.util.*;
import net.goui.cosmicdungeon.client.screen.skills.PanelScroll;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;
import net.goui.cosmicdungeon.client.screen.requests.SupplyRequestAction.Decision;

/** Pure bounded consent, variable-height card geometry and pointer ownership; no inventory mutation or prediction. */
public final class SupplyRequestsState {
    public record Geometry(Rect panel,Rect header,Rect body,Rect track,Rect acceptAll,Rect denyAll){}
    public record CardBox(Rect panel,Rect heading,List<Rect> ingredients,Rect yield,Rect accept,Rect deny){}
    public record Press(boolean consumed,SupplyRequestAction action){static final Press NONE=new Press(false,null);}
    private SupplyRequestsSnapshot snapshot=SupplyRequestsSnapshot.empty();
    private Rect bounds=new Rect(0,0,0,0);
    private Geometry geometry;private int[] starts=new int[0],heights=new int[0];
    private final PanelScroll scroll=new PanelScroll();
    private boolean connected,pending,owned;private int auxiliary;
    public SupplyRequestsState(){rebuild();}
    public SupplyRequestsSnapshot snapshot(){return snapshot;}
    public Geometry geometry(){return geometry;}
    public PanelScroll scroll(){return scroll;}
    public boolean pending(){return pending;}
    public boolean captured(){return owned||scroll.captured();}
    public boolean visible(){return bounds.width()>=36&&bounds.height()>=18;}
    public void connected(boolean value){connected=value;if(!value){pending=false;release();}}
    public void receive(SupplyRequestsSnapshot next){
        Objects.requireNonNull(next);pending=false; // Even an unchanged authoritative response acknowledges a rejected action.
        if(next.runId()==snapshot.runId()&&next.revision()<snapshot.revision())return;
        if(next.runId()!=snapshot.runId()){release();scroll.reset();}
        if(!next.equals(snapshot)){snapshot=next;rebuild();}
    }
    public void clear(){snapshot=SupplyRequestsSnapshot.empty();pending=false;release();scroll.reset();rebuild();}
    public void resize(Rect next){if(!next.equals(bounds)){release();bounds=next;rebuild();}}
    private boolean wide(){return Math.max(0,bounds.width()-10)>=110;}
    private int columns(){return Math.max(1,(Math.max(0,bounds.width()-10)-8)/18);}
    private int iconHeight(int count){return Math.max(18,((count+columns()-1)/columns())*18);}
    private void rebuild(){
        int header=Math.min(18,bounds.height()),footer=Math.min(40,bounds.height()-header),half=footer/2;
        var head=new Rect(bounds.x(),bounds.y(),bounds.width(),header);
        var body=new Rect(bounds.x(),bounds.y()+header,bounds.width(),bounds.height()-header-footer);
        int buttonWidth=Math.max(0,bounds.width()-4),buttonX=bounds.x()+Math.min(2,bounds.width());
        geometry=new Geometry(bounds,head,body,new Rect(Math.max(body.x(),body.right()-6),body.y(),Math.min(6,body.width()),body.height()),
                new Rect(buttonX,body.bottom()+1,buttonWidth,Math.max(0,half-2)),
                new Rect(buttonX,body.bottom()+half+1,buttonWidth,Math.max(0,footer-half-2)));
        starts=new int[snapshot.cards().size()];heights=new int[starts.length];int total=0;
        for(int i=0;i<starts.length;i++){
            starts[i]=total;heights[i]=28+iconHeight(snapshot.cards().get(i).ingredients().size())+14+(wide()?24:44);
            total+=heights[i]+4;
        }
        scroll.configure(total,body.height());
    }
    public Rect cardBounds(int index){
        var body=geometry.body();return new Rect(body.x()+2,body.y()+starts[index]-scroll.offset(),Math.max(0,body.width()-10),heights[index]);
    }
    public CardBox card(int index){
        var data=snapshot.cards().get(index);var panel=cardBounds(index);
        int x=panel.x(),y=panel.y(),w=panel.width();var icons=new ArrayList<Rect>();
        for(int i=0;i<data.ingredients().size();i++)icons.add(new Rect(x+4+(i%columns())*18,y+28+(i/columns())*18,16,16));
        int labelY=y+28+iconHeight(data.ingredients().size()),buttonsY=labelY+14,bw=Math.max(0,w-8);
        var accept=new Rect(x+4,buttonsY,wide()?Math.max(0,(bw-4)/2):bw,18);
        var deny=wide()?new Rect(accept.right()+4,buttonsY,Math.max(0,bw-accept.width()-4),18)
                :new Rect(x+4,buttonsY+20,bw,18);
        return new CardBox(panel,new Rect(x+4,y+3,Math.max(0,w-8),22),List.copyOf(icons),
                new Rect(x+4,labelY,Math.max(0,w-8),12),accept,deny);
    }
    public boolean canRequest(){return connected&&!pending&&snapshot.active()&&snapshot.alive()&&snapshot.canRequest();}
    public boolean enabled(Decision decision,UUID id){
        if(!connected||pending||!snapshot.active())return false;
        return switch(decision){
            case REQUEST->canRequest();
            case ACCEPT_ALL->snapshot.alive()&&snapshot.cards().stream().anyMatch(SupplyRequestsSnapshot.Card::canAccept);
            case DENY_ALL->!snapshot.cards().isEmpty();
            case ACCEPT,DENY->snapshot.cards().stream().anyMatch(card->card.requestId().equals(id)
                    &&(decision==Decision.DENY||snapshot.alive()&&card.canAccept()));
        };
    }
    public Optional<SupplyRequestAction> decide(Decision decision,UUID id){
        if(!enabled(decision,id))return Optional.empty();
        List<UUID> ids=switch(decision){
            case REQUEST->List.of();
            case ACCEPT,DENY->List.of(id);
            case ACCEPT_ALL,DENY_ALL->snapshot.cards().stream().map(SupplyRequestsSnapshot.Card::requestId).toList();
        };
        pending=true;return Optional.of(new SupplyRequestAction(snapshot.runId(),snapshot.revision(),decision,ids));
    }
    public boolean owns(double x,double y,boolean carrying){return visible()&&!carrying&&(captured()||bounds.contains(x,y));}
    public Press press(double x,double y,int button,boolean carrying){
        if(!visible()||carrying||!Double.isFinite(x)||!Double.isFinite(y)||!bounds.contains(x,y)&&!captured())return Press.NONE;
        if(button!=0){if(button>0&&button<8)auxiliary|=1<<button;return new Press(true,null);}
        owned=true;
        if(!bounds.contains(x,y))return new Press(true,null);
        if(geometry.acceptAll().contains(x,y))return new Press(true,decide(Decision.ACCEPT_ALL,null).orElse(null));
        if(geometry.denyAll().contains(x,y))return new Press(true,decide(Decision.DENY_ALL,null).orElse(null));
        if(geometry.body().contains(x,y)){
            if(geometry.track().contains(x,y)&&scroll.press(geometry.track().y(),y))return new Press(true,null);
            for(int i=0;i<snapshot.cards().size();i++){
                if(!cardBounds(i).contains(x,y))continue;
                var box=card(i);var id=snapshot.cards().get(i).requestId();
                if(box.accept().contains(x,y))return new Press(true,decide(Decision.ACCEPT,id).orElse(null));
                if(box.deny().contains(x,y))return new Press(true,decide(Decision.DENY,id).orElse(null));
            }
        }
        return new Press(true,null);
    }
    public boolean drag(double y,int button,boolean carrying){
        if(!captured()||button!=0)return false;
        if(carrying){release();return false;}
        scroll.drag(geometry.track().y(),y);return true;
    }
    public boolean wheel(double x,double y,double delta,boolean carrying){
        if(!visible()||carrying||!bounds.contains(x,y)||!Double.isFinite(delta))return false;
        if(geometry.body().contains(x,y))scroll.wheel(delta);
        return true; // Endpoint ownership prevents inventory/HUD/Skills double scrolling.
    }
    public boolean release(int button){
        if(button==0)return release();
        if(button<0||button>=8)return false;
        int bit=1<<button;boolean had=(auxiliary&bit)!=0;auxiliary&=~bit;return had;
    }
    public boolean release(){boolean had=captured();owned=false;auxiliary=0;scroll.release();return had;}
}
