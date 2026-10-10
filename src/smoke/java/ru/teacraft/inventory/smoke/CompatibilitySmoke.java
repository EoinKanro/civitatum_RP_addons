package ru.teacraft.inventory.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import ru.teacraft.inventory.client.*;

/** Exercises real packets, native menus, item synchronization and map lifecycle. */
final class CompatibilitySmoke {
    private int stage,ticks,menuId;
    private Slot accessory;
    CompatibilitySmoke(){if(Boolean.getBoolean("inventory.smoke.accessoriesOnly"))stage=18;}
    CompatibilitySmoke(int start){stage=start;}
    boolean tick(Minecraft mc) {
        ticks++;
        if(ticks<22)return false;
        if(ticks>600)throw new IllegalStateException("COMPAT timeout stage "+stage+" screen "+mc.screen);
        if(stage==0){
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                var pack=pack();var contents=NonNullList.withSize(24,ItemStack.EMPTY);contents.set(0,new ItemStack(Items.EMERALD,7));
                pack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));p.setItemSlot(EquipmentSlot.CHEST,pack);
                p.getInventory().setItem(1,ItemStack.EMPTY);p.getInventory().setItem(9,new ItemStack(Items.DIAMOND,2));p.inventoryMenu.broadcastChanges();
            });next();
        }else if(stage==1){mc.setScreen(new InventoryScreen(mc.player));next();
        }else if(stage==2&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(!CompatBridge.backpack(mc.player).isEmpty(),"Camping equipped backpack detected");check(screen.integrationKind()==CompatBridge.Kind.BACKPACK,"equipped backpack opens automatically");next();
        }else if(stage==3&&mc.screen instanceof CivitatumInventoryScreen screen&&screen.integrationKind()==CompatBridge.Kind.BACKPACK){
            check(screen.compatibilitySlots().size()==24,"24 native backpack slots embedded in Inventory");bounds(screen);capture(mc,"08-camping.png");
            var slot=screen.compatibilitySlots().getFirst();check(slot.getItem().getCount()==7,"backpack contents received from server");click(screen,slot);next();
        }else if(stage==4&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(screen.getMenu().getCarried().getCount()==7,"backpack pickup works");click(screen,inventorySlot(screen,1));next();
        }else if(stage==5&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(screen.getMenu().getCarried().isEmpty()&&mc.player.getInventory().getItem(1).getCount()==7,"backpack transfer conserves items");
            check(screen.powersPanel()!=null,"separate gifts window replaces map");next();
        }else if(stage==6&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(screen.powersPanel()!=null,"separate gifts window replaces map");next();
        }else if(stage==7&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(screen.getMenu().containerId==menuId&&mc.player.containerMenu==screen.getMenu(),"Xaero returns to same live backpack menu");
            check(mc.player.getInventory().getItem(1).getCount()==7,"map return preserves inventory state");screen.onClose();next();
        }else if(stage==8){check(CompatBridge.openBackpack(),"reopen Camping request sent");next();
        }else if(stage==9&&mc.screen instanceof CivitatumInventoryScreen screen&&screen.integrationKind()==CompatBridge.Kind.BACKPACK){
            check(screen.compatibilitySlots().getFirst().getItem().isEmpty(),"backpack changes persisted after closing");screen.onClose();
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.getInventory().setItem(9,pack());p.inventoryMenu.broadcastChanges();});next();
        }else if(stage==10){check(CompatBridge.openCurios(),"Curios request sent");next();
        }else if(stage==11&&mc.screen instanceof CivitatumInventoryScreen screen&&screen.integrationKind()==CompatBridge.Kind.CURIOS){
            check(screen.compatibilitySlots().size()==48,"Curios first native page contains 48 slots");bounds(screen);capture(mc,"10-curios.png");
            var inv=inventorySlot(screen,9);mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId,inv.index,0,ClickType.PICKUP,mc.player);next();
        }else if(stage==12&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(screen.getMenu().getCarried().getItem()==pack().getItem(),"Curios native inventory pickup");click(screen,screen.compatibilitySlots().getFirst());next();
        }else if(stage==13&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(screen.compatibilitySlots().getFirst().getItem().getItem()==pack().getItem()&&screen.getMenu().getCarried().isEmpty(),"Curios equip synchronized");
            check(!CompatBridge.backpack(mc.player).isEmpty(),"Camping detects backpack equipped in Curios");
            check(CompatBridge.changeCuriosPage(screen.getMenu(),true),"Curios page request sent");next();
        }else if(stage==14&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(CompatBridge.integer(screen.getMenu(),"currentPage",-1)==1&&screen.compatibilitySlots().size()==22,"Curios server page switch rebuilt 22 native slots");bounds(screen);
            CompatBridge.cosmetics(screen.getMenu());next();
        }else if(stage==15&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(CompatBridge.flag(screen.getMenu(),"isViewingCosmetics"),"Curios cosmetic view synchronized");CompatBridge.changeCuriosPage(screen.getMenu(),false);next();
        }else if(stage==16&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(CompatBridge.integer(screen.getMenu(),"currentPage",-1)==0,"Curios previous page works");screen.onClose();check(CompatBridge.openBackpack(),"Camping opens Curios-equipped backpack");next();
        }else if(stage==17&&mc.screen instanceof CivitatumInventoryScreen screen&&screen.integrationKind()==CompatBridge.Kind.BACKPACK){
            check(screen.compatibilitySlots().size()==24,"Curios backpack remains usable through Camping");screen.onClose();next();
        }else if(stage==18){
            if(!CompatBridge.loaded("accessories"))return true;
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.getInventory().setItem(9,new ItemStack(Items.DIAMOND,2));p.inventoryMenu.broadcastChanges();});
            check(CompatBridge.openAccessories(),"Accessories native screen request sent");next();
        }else if(stage==19&&mc.screen instanceof CivitatumInventoryScreen screen&&screen.integrationKind()==CompatBridge.Kind.ACCESSORIES){
            check(!screen.compatibilitySlots().isEmpty(),"Accessories native slots embedded");bounds(screen);capture(mc,"11-accessories.png");
            System.out.println("ACCESSORIES_SLOT_CLASSES "+screen.getMenu().slots.stream().map(s->s.getClass().getName()).distinct().toList());
            check(screen.getMenu().slots.stream().filter(s->CompatBridge.accessoryArmor(s)>=0).count()==4,"four native Accessories armor slots available");
            for(var slot:screen.getMenu().slots)if(CompatBridge.accessoryArmor(slot)>=0)check(screen.equipmentPanel().contains(slot.x,slot.y),"Accessories armor stays in equipment panel");
            accessory=screen.compatibilitySlots().stream().filter(s->s.mayPlace(new ItemStack(Items.DIAMOND))).findFirst().orElseThrow();
            mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId,inventorySlot(screen,9).index,0,ClickType.PICKUP,mc.player);next();
        }else if(stage==20&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(screen.getMenu().getCarried().is(Items.DIAMOND),"Accessories pickup from player inventory");
            mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId,accessory.index,1,ClickType.PICKUP,mc.player);next();
        }else if(stage==21&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(accessory.getItem().is(Items.DIAMOND)&&accessory.getItem().getCount()==1,"Accessories equip synchronized");
            check(screen.getMenu().getCarried().getCount()==1,"Accessories partial transfer conserves count");click(screen,inventorySlot(screen,2));next();
        }else if(stage==22&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(screen.getMenu().getCarried().isEmpty(),"Accessories remainder deposited");press(screen,"Косметика");next();
        }else if(stage==23&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(screen.compatibilitySlots().stream().allMatch(CompatBridge::cosmeticSlot),"Accessories cosmetic slots shown");bounds(screen);capture(mc,"12-accessories-cosmetics.png");
            press(screen,"Косметика");screen.onClose();check(CompatBridge.openAccessories(),"Accessories reopen request");next();
        }else if(stage==24&&mc.screen instanceof CivitatumInventoryScreen screen&&screen.integrationKind()==CompatBridge.Kind.ACCESSORIES){
            check(screen.compatibilitySlots().stream().anyMatch(s->s.getItem().is(Items.DIAMOND)),"Accessories items persisted after closing");
            check(screen.equipmentPanel().x+screen.equipmentPanel().width==screen.itemsPanel().x,"Accessories keeps equipment docked");
            screen.onClose();mc.setScreen(new InventoryScreen(mc.player));mc.getToasts().clear();mc.gui.getChat().clearMessages(true);next();
        }else if(stage==25&&mc.screen instanceof CivitatumInventoryScreen screen){
            screen.onClose();check(CompatBridge.openAccessories("ORIGINAL"),"Accessories classic menu request");next();
        }else if(stage==26&&mc.screen instanceof CivitatumInventoryScreen screen&&CompatBridge.originalAccessories(screen.getMenu())){
            check(screen.compatibilitySlots().size()>0&&screen.compatibilitySlots().size()<=8,"classic Accessories uses native visible slots");
            CompatBridge.accessoryPage(screen.getMenu(),true);next();
        }else if(stage==27&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(CompatBridge.integer(screen.getMenu(),"scrolledIndex",0)==8,"Accessories classic scrolling synchronized with server");bounds(screen);
            CompatBridge.accessoryCosmetics(screen.getMenu());next();
        }else if(stage==28&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(CompatBridge.accessoriesCosmetics(screen.getMenu())&&screen.compatibilitySlots().stream().allMatch(CompatBridge::cosmeticSlot),"Accessories classic cosmetic state synchronized");
            var event=new net.neoforged.neoforge.client.event.ScreenEvent.KeyPressed.Pre(screen,297,0,0);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);next();
        }else if(stage==29&&mc.screen!=null&&!(mc.screen instanceof CivitatumInventoryScreen)){
            check(!InventoryPreferences.get().enabled,"Accessories F8 opens native screen");
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.KeyPressed.Pre(mc.screen,297,0,0));next();
        }else if(stage==30&&mc.screen instanceof CivitatumInventoryScreen screen&&screen.integrationKind()==CompatBridge.Kind.ACCESSORIES){
            check(InventoryPreferences.get().enabled&&!screen.compatibilitySlots().isEmpty(),"Accessories F8 restores fresh interactive menu");
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,960);mc.options.guiScale().set(4);mc.resizeDisplay();next();
        }else if(stage==31&&mc.screen instanceof CivitatumInventoryScreen screen){mc.options.guiScale().set(4);mc.resizeDisplay();next();
        }else if(stage==32&&mc.screen instanceof CivitatumInventoryScreen screen){
            check(screen.width==320&&screen.height==240,"Accessories compact GUI 320x240");bounds(screen);
            var p=screen.itemsPanel();screen.mouseScrolled(p.x+30,p.y+105,0,-3);bounds(screen);capture(mc,"13-accessories-compact.png");
            screen.onClose();mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new InventoryScreen(mc.player));mc.getToasts().clear();mc.gui.getChat().clearMessages(true);next();
        }else if(stage==33&&mc.screen instanceof CivitatumInventoryScreen screen){capture(mc,"00-overview.png");System.out.println("INVENTORY_COMPAT_ALL_PASS");return true;}
        return false;
    }
    private void next(){stage++;ticks=0;System.out.println("COMPAT_STAGE "+stage);}
    private static void check(boolean ok,String what){if(!ok)throw new IllegalStateException("COMPAT FAIL: "+what);System.out.println("COMPAT PASS: "+what);}
    private static ItemStack pack(){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("camping:small_backpack")));}
    private static Slot inventorySlot(CivitatumInventoryScreen s,int index){return s.getMenu().slots.stream().filter(slot->slot.container==Minecraft.getInstance().player.getInventory()&&slot.getContainerSlot()==index).findFirst().orElseThrow();}
    private static void click(CivitatumInventoryScreen screen,Slot slot){screen.mouseClicked(slot.x+8,slot.y+8,0);screen.mouseReleased(slot.x+8,slot.y+8,0);}
    private static void press(CivitatumInventoryScreen screen,String label){
        var button=(Button)screen.children().stream().filter(c->c instanceof Button b&&b.visible&&b.getMessage().getString().equals(label)).findFirst().orElseThrow();
        check(button.active,"button "+label+" active");int x=button.getX()+button.getWidth()/2,y=button.getY()+9;screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);
    }
    private static void bounds(CivitatumInventoryScreen screen){for(var slot:screen.getMenu().slots)if(slot.x>=0)check(slot.x+16<=screen.width&&slot.y>=24&&slot.y+16<screen.height-20,"compatibility slot "+slot.index+" within bounds");}
    private static void capture(Minecraft mc,String name){var dir=new java.io.File("../build/client-captures");dir.mkdirs();net.minecraft.client.Screenshot.grab(dir,name,mc.getMainRenderTarget(),msg->System.out.println("INVENTORY_CAPTURE "+name));}
}
