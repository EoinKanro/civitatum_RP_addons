package ru.teacraft.inventory.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import ru.teacraft.inventory.client.*;

/** Real GUI clicks and server-owned containers exercise the 0.3 changes together. */
final class RevisionSmoke {
    private int stage,ticks,menuId;
    private SimpleContainer chest;
    private CompatibilitySmoke legacy;
    private Slot ring;
    boolean tick(Minecraft mc){
        ticks++;mc.getToasts().clear();mc.gui.getChat().clearMessages(true);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        if(stage==28){if(legacy.tick(mc)){System.out.println("INVENTORY_REVISION_ALL_PASS");return true;}return false;}
        if(ticks<30)return false;if(ticks>700)throw new IllegalStateException("REVISION timeout "+stage+" "+mc.screen);
        if(stage==0){
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                p.getInventory().clearContent();p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(p.getX(),p.getY()+5,p.getZ());
                var pack=pack();var contents=NonNullList.withSize(24,ItemStack.EMPTY);contents.set(0,new ItemStack(Items.EMERALD,7));pack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));
                p.setItemSlot(EquipmentSlot.CHEST,pack);p.getInventory().setItem(9,new ItemStack(Items.DIAMOND,6));p.getInventory().setItem(10,ItemStack.EMPTY);p.getInventory().setItem(0,new ItemStack(Items.TORCH,64));p.getInventory().setItem(13,new ItemStack(Items.OAK_LOG,16));p.getInventory().setItem(14,new ItemStack(Items.COBBLESTONE,32));p.getInventory().setItem(15,new ItemStack(Items.APPLE,16));p.getInventory().setItem(16,new ItemStack(Items.IRON_SWORD));p.getInventory().setItem(17,new ItemStack(Items.ARROW,32));p.inventoryMenu.broadcastChanges();
            });next();
        }else if(stage==1){mc.setScreen(new InventoryScreen(mc.player));next();
        }else if(stage==2&&mc.screen instanceof CivitatumInventoryScreen s&&s.integrationKind()==CompatBridge.Kind.BACKPACK){
            check(s.compatibilitySlots().size()==24,"Camping native slots");check(inv(s,9).x>=0&&s.compatibilitySlots().getFirst().x>=0,"main inventory and backpack visible together");bounds(s);capture(mc,"20-joint-backpack-map.png");click(s,s.compatibilitySlots().getFirst());next();
        }else if(stage==3&&mc.screen instanceof CivitatumInventoryScreen s){check(s.getMenu().getCarried().getCount()==7,"backpack pickup");click(s,inv(s,10));next();
        }else if(stage==4&&mc.screen instanceof CivitatumInventoryScreen s){check(mc.player.getInventory().getItem(10).getCount()==7&&s.getMenu().getCarried().isEmpty(),"backpack to regular inventory");click(s,inv(s,9));next();
        }else if(stage==5&&mc.screen instanceof CivitatumInventoryScreen s){check(s.getMenu().getCarried().getCount()==6,"regular inventory pickup");click(s,s.compatibilitySlots().get(1));next();
        }else if(stage==6&&mc.screen instanceof CivitatumInventoryScreen s){check(s.compatibilitySlots().get(1).getItem().getCount()==6,"regular inventory to backpack");mc.gameMode.handleInventoryMouseClick(s.getMenu().containerId,s.compatibilitySlots().get(1).index,0,ClickType.QUICK_MOVE,mc.player);next();
        }else if(stage==7&&mc.screen instanceof CivitatumInventoryScreen s){
            check(s.compatibilitySlots().get(1).getItem().isEmpty(),"backpack shift transfer");int count=0;for(int i=0;i<36;i++)if(mc.player.getInventory().getItem(i).is(Items.DIAMOND))count+=mc.player.getInventory().getItem(i).getCount();check(count==6,"shift transfer conserves count");
            check(s.powersPanel()!=null,"separate gifts window replaces map");next();
        }else if(stage==8&&mc.screen instanceof CivitatumInventoryScreen s){
            check(s.powersPanel()!=null,"separate gifts window replaces map");next();
        }else if(stage==9&&mc.screen instanceof CivitatumInventoryScreen s){
            var p=s.informationPanel();
            for(int edge:new int[]{1,2,4,8,5,6,9,10}){
                point(s,p.x+10,p.y+9,0);int x=(edge&1)!=0?p.x+1:(edge&2)!=0?p.x+p.width-2:p.x+p.width/2;
                int y=(edge&4)!=0?p.y+1:(edge&8)!=0?p.y+p.height-2:p.y+p.height/2;
                int w=p.width,h=p.height;s.mouseClicked(x,y,0);s.mouseDragged(x+((edge&3)!=0?2:0),y+((edge&12)!=0?2:0),0,2,2);s.mouseReleased(x+2,y+2,0);
                check(((edge&3)==0||p.width!=w)&&((edge&12)==0||p.height!=h),"resize edge/corner "+edge);
            }
            int ox=p.x,oy=p.y;var target=inv(s,10);p.x=target.x-6;p.y=target.y-12;point(s,p.x+10,p.y+9,0);
            check(s.pickSlot(target.x+8,target.y+8)==null,"upper window blocks lower slot input");int count=target.getItem().getCount();click(s,target);check(target.getItem().getCount()==count&&s.getMenu().getCarried().isEmpty(),"covered slot unchanged");capture(mc,"22-overlap.png");p.x=ox;p.y=oy;
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,960);mc.options.guiScale().set(4);mc.resizeDisplay();next();
        }else if(stage==10&&mc.screen instanceof CivitatumInventoryScreen s){mc.options.guiScale().set(4);mc.resizeDisplay();next();
        }else if(stage==11&&mc.screen instanceof CivitatumInventoryScreen s){
            check(s.width==320&&s.height==240,"joint compact screen");check(inv(s,9).x>=0&&s.compatibilitySlots().getFirst().x>=0,"both grids visible at minimum scale");bounds(s);
            s.mouseScrolled(inv(s,9).x+5,inv(s,9).y+5,0,-3);s.mouseScrolled(s.compatibilitySlots().getFirst().x+5,s.compatibilitySlots().getFirst().y+5,0,-3);
            check(inv(s,27).x>=0&&s.compatibilitySlots().get(23).x>=0,"independent scrolling reaches both grids");bounds(s);capture(mc,"27-joint-compact.png");s.onClose();mc.options.guiScale().set(2);mc.resizeDisplay();next();
        }else if(stage==12){next();
        }else if(stage==13){
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());chest=new SimpleContainer(54);chest.setItem(0,new ItemStack(Items.IRON_INGOT,8));p.openMenu(new SimpleMenuProvider((id,inv,player)->ChestMenu.sixRows(id,inv,chest),Component.literal("Большой сундук")));});next();
        }else if(stage==14&&mc.screen instanceof CivitatumInventoryScreen s&&s.integrationKind()==CompatBridge.Kind.BLOCK){check(s.compatibilitySlots().size()==54,"native large chest slots");bounds(s);capture(mc,"23-chest.png");click(s,s.compatibilitySlots().getFirst());next();
        }else if(stage==15&&mc.screen instanceof CivitatumInventoryScreen s){check(s.getMenu().getCarried().getCount()==8,"chest pickup");click(s,inv(s,11));next();
        }else if(stage==16&&mc.screen instanceof CivitatumInventoryScreen s){check(mc.player.getInventory().getItem(11).getCount()==8&&chest.getItem(0).isEmpty(),"chest transfer verified on server");s.onClose();next();
        }else if(stage==17){
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var c=new SimpleContainer(3);c.setItem(0,new ItemStack(Items.IRON_ORE));c.setItem(1,new ItemStack(Items.COAL,8));c.setItem(2,new ItemStack(Items.IRON_INGOT,3));var d=new SimpleContainerData(4);d.set(0,100);d.set(1,200);d.set(2,50);d.set(3,200);p.openMenu(new SimpleMenuProvider((id,inv,player)->new FurnaceMenu(id,inv,c,d),Component.literal("Печь")));});next();
        }else if(stage==18&&mc.screen instanceof CivitatumInventoryScreen s&&s.getMenu() instanceof FurnaceMenu){check(s.compatibilitySlots().size()==3,"native furnace slots");check(Math.abs(CompatBridge.progress(s.getMenu(),"getBurnProgress",1)-0.25)<0.01,"furnace progress");bounds(s);capture(mc,"24-furnace.png");click(s,s.compatibilitySlots().get(2));next();
        }else if(stage==19&&mc.screen instanceof CivitatumInventoryScreen s){check(s.getMenu().getCarried().getCount()==3,"furnace output pickup");click(s,inv(s,12));next();
        }else if(stage==20&&mc.screen instanceof CivitatumInventoryScreen s){check(mc.player.getInventory().getItem(12).getCount()==3,"furnace output transfer");s.onClose();next();
        }else if(stage==21){
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.getInventory().setItem(9,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("aether:iron_ring"))));p.inventoryMenu.broadcastChanges();});check(CompatBridge.openAether(),"Aether packet");next();
        }else if(stage==22&&mc.screen instanceof CivitatumInventoryScreen s&&s.integrationKind()==CompatBridge.Kind.AETHER){check(s.compatibilitySlots().size()==8,"eight real Aether accessory slots");for(var slot:s.compatibilitySlots())check(s.equipmentPanel().contains(slot.x,slot.y),"Aether slot in equipment window");bounds(s);ring=s.compatibilitySlots().stream().filter(slot->slot.mayPlace(inv(s,9).getItem())).findFirst().orElseThrow();click(s,inv(s,9));next();
        }else if(stage==23&&mc.screen instanceof CivitatumInventoryScreen s){check(!s.getMenu().getCarried().isEmpty(),"Aether ring pickup");click(s,ring);next();
        }else if(stage==24&&mc.screen instanceof CivitatumInventoryScreen s){check(!ring.getItem().isEmpty()&&s.getMenu().getCarried().isEmpty(),"Aether ring equip synchronized");capture(mc,"25-aether.png");check(AttributeView.entries(mc.player,false).size()>20,"Apothic attributes present");check(!AttributeView.entries(mc.player,true).isEmpty()&&AttributeView.entries(mc.player,true).size()<AttributeView.entries(mc.player,false).size(),"attribute modification filter");press(s,"Атриб.");next();
        }else if(stage==25){capture(mc,"26-apothic-attributes.png");((CivitatumInventoryScreen)mc.screen).onClose();next();
        }else if(stage==26){mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.getInventory().setItem(9,pack());p.inventoryMenu.broadcastChanges();});next();
        }else if(stage==27){legacy=new CompatibilitySmoke(10);next();}
        return false;
    }
    private void next(){stage++;ticks=0;System.out.println("REVISION_STAGE "+stage);}
    private static void check(boolean ok,String text){if(!ok)throw new IllegalStateException("REVISION FAIL: "+text);System.out.println("REVISION PASS: "+text);}
    private static ItemStack pack(){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("camping:small_backpack")));}
    private static Slot inv(CivitatumInventoryScreen s,int id){return s.getMenu().slots.stream().filter(slot->slot.container==Minecraft.getInstance().player.getInventory()&&slot.getContainerSlot()==id).findFirst().orElseThrow();}
    private static void click(CivitatumInventoryScreen s,Slot slot){point(s,slot.x+8,slot.y+8,0);}
    private static void point(CivitatumInventoryScreen s,int x,int y,int b){s.mouseClicked(x,y,b);s.mouseReleased(x,y,b);}
    private static void press(CivitatumInventoryScreen s,String label){var b=(Button)s.children().stream().filter(w->w instanceof Button button&&button.visible&&button.getMessage().getString().equals(label)).findFirst().orElseThrow();point(s,b.getX()+b.getWidth()/2,b.getY()+9,0);}
    private static void bounds(CivitatumInventoryScreen s){for(var slot:s.getMenu().slots)if(slot.x>=0)check(slot.x+16<=s.width&&slot.y>=24&&slot.y+16<s.height-20,"slot "+slot.index+" within bounds");}
    private static void capture(Minecraft mc,String name){var dir=new java.io.File("../build/client-captures");dir.mkdirs();net.minecraft.client.Screenshot.grab(dir,name,mc.getMainRenderTarget(),message->System.out.println("INVENTORY_CAPTURE "+name));}
}
