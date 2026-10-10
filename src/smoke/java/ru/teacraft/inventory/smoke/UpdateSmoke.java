package ru.teacraft.inventory.smoke;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import ru.teacraft.inventory.client.*;

/** 0.4 regressions use live slots, actual mouse events, controls and synchronized Ars mana. */
final class UpdateSmoke {
    private int stage,ticks,casts;
    private Slot source,target;
    private volatile int expectedManaMax;
    boolean tick(Minecraft mc){
        ticks++;if(ticks<30)return false;if(ticks>600)throw new IllegalStateException("UPDATE FAIL: timeout "+stage);
        if(stage==0){
            var s=(CivitatumInventoryScreen)mc.screen;s.onClose();
            InventoryPreferences.get().panels.clear();InventoryPreferences.get().slotScale=1;InventoryPreferences.get().powersVisible=true;
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.getInventory().setItem(0,new ItemStack(Items.MACE));p.getInventory().setItem(9,new ItemStack(Items.EMERALD,7));p.getInventory().setItem(10,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();
            });next();
        }else if(stage==1){mc.setScreen(new InventoryScreen(mc.player));next();
        }else if(stage==2&&mc.screen instanceof CivitatumInventoryScreen s){
            var p=s.itemsPanel();point(s,p.x+20,p.y+55);for(char c:"булава".toCharArray())s.charTyped(c,0);
            check(s.matches(new ItemStack(Items.MACE)),"Russian mace search");check(s.highlighted(inv(s,0)),"hotbar match is highlighted");
            point(s,p.x+12,p.y+p.height-21); // blank click raises panel; icons must remain visible.
            next();
        }else if(stage==3&&mc.screen instanceof CivitatumInventoryScreen s){
            capture(mc,"40-mace-search-after-click.png");
            var slider=(AbstractSliderButton)s.children().stream().filter(w->w instanceof AbstractSliderButton a&&a.getMessage().getString().startsWith("Размер")).findFirst().orElseThrow();
            point(s,slider.getX()+slider.getWidth()-5,slider.getY()+9);
            check(InventoryPreferences.get().slotScale>1.9,"slot slider updates preference");
            source=inv(s,9);target=inv(s,10);check(s.slotPixels(source)>=30,"inventory items scale with slot slider");
            check(s.pickSlot(source.x+26,source.y+20)==source,"scaled slot hitbox extends past 16 pixels");
            point(s,source.x+26,source.y+20);next();
        }else if(stage==4&&mc.screen instanceof CivitatumInventoryScreen s){
            check(s.getMenu().getCarried().getCount()==7,"scaled slot pickup reaches server");point(s,target.x+25,target.y+20);next();
        }else if(stage==5&&mc.screen instanceof CivitatumInventoryScreen s){
            check(target.getItem().getCount()==7&&s.getMenu().getCarried().isEmpty(),"scaled slot transfer conserves items");capture(mc,"41-scaled-slots.png");
            var slider=(AbstractSliderButton)s.children().stream().filter(w->w instanceof AbstractSliderButton a&&a.getMessage().getString().startsWith("Прозрачность")).findFirst().orElseThrow();
            point(s,slider.getX()+slider.getWidth()-4,slider.getY()+9);check(InventoryPreferences.get().transparency>0.85,"opacity slider updates preference");
            InventoryPreferences.get().slotScale=1;InventoryPreferences.get().transparency=.6;
            press(s,"Атриб.");next();
        }else if(stage==6&&mc.screen instanceof CivitatumInventoryScreen s){
            var p=s.informationPanel();cursor(mc,p.x+30,p.y+57);next();
        }else if(stage==7&&mc.screen instanceof CivitatumInventoryScreen s){
            capture(mc,"42-attribute-side-tooltip.png");
            AbilitySelection.register("inventory_smoke",player->List.of(
                new AbilitySelection.Ability("inventory_smoke:first",Component.literal("Проверочный дар"),new ItemStack(Items.NETHER_STAR),List.of(Component.literal("Проверка выбора дара")),()->true,()->casts++),
                new AbilitySelection.Ability("inventory_smoke:second",Component.literal("Проверочное заклинание"),new ItemStack(Items.ENCHANTED_BOOK),List.of(Component.literal("Проверка переключения")),()->true,()->casts++)));
            AbilitySelection.select("inventory_smoke:first");var powers=s.powersPanel();point(s,powers.x+12,powers.y+8);cursor(mc,powers.x+15,powers.y+45);next();
        }else if(stage==8&&mc.screen instanceof CivitatumInventoryScreen s){
            capture(mc,"43-gifts-effects-and-selection.png");check(mc.player.getActiveEffects().size()>=2,"real active effects available in gifts window");s.onClose();
            KeyMapping.click(InventoryClient.NEXT.getKey());next();
        }else if(stage==9){
            check(AbilitySelection.selected(mc.player).id().equals("inventory_smoke:second"),"next ability key cycles selection");KeyMapping.click(InventoryClient.PREVIOUS.getKey());next();
        }else if(stage==10){
            check(AbilitySelection.selected(mc.player).id().equals("inventory_smoke:first"),"previous ability key cycles selection");KeyMapping.click(InventoryClient.ACTIVATE.getKey());next();
        }else if(stage==11){
            check(casts==1,"activation key invokes only selected ability");check(mc.screen==null,"activation does not trigger Curios inventory key");check(CivitatumHud.active(),"custom HUD active in survival");
            if(CompatBridge.loaded("ars_nouveau")){
                mc.getSingleplayerServer().execute(()->{
                    try{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var cap=Class.forName("com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry").getMethod("getMana",net.minecraft.world.entity.LivingEntity.class).invoke(null,p);expectedManaMax=((Number)cap.getClass().getMethod("getMaxMana").invoke(cap)).intValue();cap.getClass().getMethod("setMana",double.class).invoke(cap,expectedManaMax*.5);cap.getClass().getMethod("syncToClient",net.minecraft.server.level.ServerPlayer.class).invoke(cap,p);}catch(Exception e){throw new IllegalStateException(e);}
                });
            }next();
        }else if(stage==12){
            if(CompatBridge.loaded("ars_nouveau")){var mana=ArsBridge.mana(mc.player);check(mana!=null&&mana.max()==expectedManaMax&&mana.current()>=expectedManaMax*.5&&mana.current()<expectedManaMax,"Ars client mana reflects server attachment");}
            capture(mc,"44-morrowind-hud.png");
            mc.setScreen(new InventoryScreen(mc.player));next();
        }else if(stage==13){
            var s=(CivitatumInventoryScreen)mc.screen;s.onClose();
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,player)->new CraftingMenu(id,inv),Component.literal("Верстак")));});next();
        }else if(stage==14&&mc.screen instanceof CivitatumInventoryScreen s&&s.getMenu() instanceof CraftingMenu){
            check(s.compatibilitySlots().size()==10,"crafting uses ten native slots");check(s.containerPanel().height<=160,"compact workstation default height");capture(mc,"45-workbench.png");s.onClose();next();
        }else if(stage==15){
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var c=new net.minecraft.world.SimpleContainer(3);c.setItem(0,new ItemStack(Items.IRON_ORE));c.setItem(1,new ItemStack(Items.COAL));c.setItem(2,new ItemStack(Items.IRON_INGOT,3));var d=new net.minecraft.world.inventory.SimpleContainerData(4);d.set(0,100);d.set(1,200);d.set(2,50);d.set(3,200);p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,player)->new FurnaceMenu(id,inv,c,d),Component.literal("Печь")));});next();
        }else if(stage==16&&mc.screen instanceof CivitatumInventoryScreen s&&s.getMenu() instanceof FurnaceMenu){
            check(s.compatibilitySlots().size()==3,"furnace uses native input fuel output");capture(mc,"46-furnace.png");s.onClose();next();
        }else if(stage==17){System.out.println("INVENTORY_UPDATE_ALL_PASS");return true;}
        return false;
    }
    private void next(){stage++;ticks=0;System.out.println("UPDATE_STAGE "+stage);}
    private static void check(boolean ok,String text){if(!ok)throw new IllegalStateException("UPDATE FAIL: "+text);System.out.println("UPDATE PASS: "+text);}
    private static Slot inv(CivitatumInventoryScreen s,int id){return s.getMenu().slots.stream().filter(slot->slot.container==Minecraft.getInstance().player.getInventory()&&slot.getContainerSlot()==id).findFirst().orElseThrow();}
    private static void point(CivitatumInventoryScreen s,int x,int y){s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);}
    private static void press(CivitatumInventoryScreen s,String text){var b=(Button)s.children().stream().filter(w->w instanceof Button a&&a.getMessage().getString().equals(text)).findFirst().orElseThrow();point(s,b.getX()+b.getWidth()/2,b.getY()+8);}
    private static void cursor(Minecraft mc,int x,int y){double scale=mc.getWindow().getGuiScale();org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),x*scale,y*scale);
        try{var method=mc.mouseHandler.getClass().getDeclaredMethod("onMove",long.class,double.class,double.class);method.setAccessible(true);method.invoke(mc.mouseHandler,mc.getWindow().getWindow(),x*scale,y*scale);}catch(Exception e){throw new IllegalStateException(e);}}
    private static void capture(Minecraft mc,String name){var dir=new java.io.File("../build/client-captures");dir.mkdirs();net.minecraft.client.Screenshot.grab(dir,name,mc.getMainRenderTarget(),msg->System.out.println("INVENTORY_CAPTURE "+name));}
}
