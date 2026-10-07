package net.goui.cosmicdungeon.client.screen.requests;

import java.util.*;
import net.minecraft.world.item.ItemStack;

/** Bounded authoritative quotes. Each copied stack is the exact component-bearing quantity consent can consume. */
public record SupplyRequestsSnapshot(long runId,long revision,boolean active,boolean alive,boolean canRequest,List<Card> cards) {
    public static final int MAX_CARDS=32,MAX_INGREDIENTS=64,CAP=600;
    public record Ingredient(ItemStack stack){
        public Ingredient{
            Objects.requireNonNull(stack);
            if(stack.isEmpty()||stack.getCount()>CAP)throw new IllegalArgumentException("Invalid quoted stack");
            stack=stack.copy();
        }
        @Override public ItemStack stack(){return stack.copy();}
        public int count(){return stack.getCount();}
        @Override public boolean equals(Object other){return other instanceof Ingredient ingredient&&ItemStack.matches(stack,ingredient.stack);}
        @Override public int hashCode(){return 31*ItemStack.hashItemAndComponents(stack)+stack.getCount();}
    }
    public record Card(UUID requestId,UUID requesterId,String requesterName,String requesterClass,String resourceId,
                       int yield,List<Ingredient> ingredients,boolean canAccept){
        public Card{
            Objects.requireNonNull(requestId);Objects.requireNonNull(requesterId);Objects.requireNonNull(requesterName);
            Objects.requireNonNull(requesterClass);Objects.requireNonNull(resourceId);ingredients=List.copyOf(ingredients);
            boolean role=resourceId.equals("brewing_supplies")&&requesterClass.equals("theurgist")
                    ||resourceId.equals("kibble")&&requesterClass.equals("bogatyr");
            if(!role||requesterName.isBlank()||requesterName.length()>64||yield<0||yield>CAP
                    ||ingredients.size()>MAX_INGREDIENTS||ingredients.stream().mapToInt(Ingredient::count).sum()!=yield
                    ||canAccept&&yield==0)throw new IllegalArgumentException("Invalid supply request card");
        }
        public String resourceName(){return resourceId.equals("kibble")?"Kibble":"Brewing Supplies";}
        public String className(){return requesterClass.equals("bogatyr")?"Bogatyr":"Theurgist";}
    }
    public SupplyRequestsSnapshot{
        cards=List.copyOf(cards);
        if(runId<0||revision<0||cards.size()>MAX_CARDS||cards.stream().map(Card::requestId).distinct().count()!=cards.size()
                ||runId==0&&(active||alive||canRequest||!cards.isEmpty())
                ||active&&runId==0||!active&&(!cards.isEmpty()||canRequest)
                ||canRequest&&!alive||cards.stream().anyMatch(card->card.canAccept()&&(!active||!alive)))
            throw new IllegalArgumentException("Invalid requests snapshot");
    }
    public static SupplyRequestsSnapshot empty(){return new SupplyRequestsSnapshot(0,0,false,false,false,List.of());}
}
