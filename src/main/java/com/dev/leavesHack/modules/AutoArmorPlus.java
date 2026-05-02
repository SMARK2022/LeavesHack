package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.math.Timer;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashMap;
import java.util.Map;

import static com.dev.leavesHack.utils.rotation.Rotation.sendPacket;

public class AutoArmorPlus extends Module {
    private Timer timer = new Timer();
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("Delay")
            .description("MS")
            .defaultValue(10)
            .min(0)
            .sliderMax(1000)
            .build()
    );
    private final Setting<Boolean> autoElytra = sgGeneral.add(new BoolSetting.Builder()
            .name("AutoElytra")
            .description("Automatically equips elytra when ElytraFly")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreBinding = sgGeneral.add(new BoolSetting.Builder()
            .name("IgnoreBinding")
            .description("")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> snowBug = sgGeneral.add(new BoolSetting.Builder()
            .name("SnowBug")
            .description("")
            .defaultValue(false)
            .build()
    );
    public AutoArmorPlus() {
        super(LeavesHack.CATEGORY, "AutoArmorPlus", "Automatically equips armor or elytra");
    }
    @Override
    public void onActivate() {
        timer.setMs(999999);
    }
    @EventHandler
    public void onTick(TickEvent.Pre event){
        if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof InventoryScreen) && !(mc.currentScreen instanceof WidgetScreen)) {
            return;
        }
        if (mc.player.playerScreenHandler != mc.player.currentScreenHandler) return;
        if (!timer.passedMs(delay.get())) return;
        timer.reset();
        Map<EquipmentSlot, int[]> armorMap = new HashMap<>(4);
        armorMap.put(EquipmentSlot.FEET, new int[]{36, getProtection(mc.player.getInventory().getStack(36)), -1, -1});
        armorMap.put(EquipmentSlot.LEGS, new int[]{37, getProtection(mc.player.getInventory().getStack(37)), -1, -1});
        armorMap.put(EquipmentSlot.CHEST, new int[]{38, getProtection(mc.player.getInventory().getStack(38)), -1, -1});
        armorMap.put(EquipmentSlot.HEAD, new int[]{39, getProtection(mc.player.getInventory().getStack(39)), -1, -1});
        for (int s = 0; s < 36; s++) {
            ItemStack stack = mc.player.getInventory().getStack(s);
            if (!isArmor(stack) && stack.getItem() != Items.ELYTRA)
                continue;
            int protection = getProtection(stack);
            EquipmentSlot slot = getEquipmentSlot(stack);
            if (slot == null) continue;
            for (Map.Entry<EquipmentSlot, int[]> e : armorMap.entrySet()) {
                if (e.getKey() == EquipmentSlot.FEET) {
                    if (mc.player.hurtTime > 1 && snowBug.get()) {
                        if (!mc.player.getInventory().getStack(36).isEmpty() && mc.player.getInventory().getStack(36).getItem() == Items.LEATHER_BOOTS) {
                            continue;
                        }
                        if (!mc.player.getInventory().getStack(s).isEmpty() && mc.player.getInventory().getStack(s).getItem() == Items.LEATHER_BOOTS) {
                            e.getValue()[2] = s;
                            continue;
                        }
                    }
                }
                FireworkElytraFly fireworkElytraFly = Modules.get().get(FireworkElytraFly.class);
                if (autoElytra.get() && fireworkElytraFly.isActive() && e.getKey() == EquipmentSlot.CHEST) {
                    if (!mc.player.getInventory().getStack(38).isEmpty() && isUsableElytra(mc.player.getInventory().getStack(38))) {
                        continue;
                    }
                    if (e.getValue()[2] != -1 && !mc.player.getInventory().getStack(e.getValue()[2]).isEmpty() && isUsableElytra(mc.player.getInventory().getStack(e.getValue()[2]))) {
                        continue;
                    }
                    if (!stack.isEmpty() && isUsableElytra(stack)) {
                        e.getValue()[2] = s;
                    }
                    continue;
                }
                if (protection > 0) {
                    if (e.getKey() == slot) {
                        if (protection > e.getValue()[1] && protection > e.getValue()[3]) {
                            e.getValue()[2] = s;
                            e.getValue()[3] = protection;
                        }
                    }
                }
            }
        }
        for (Map.Entry<EquipmentSlot, int[]> equipmentSlotEntry : armorMap.entrySet()) {
            if (equipmentSlotEntry.getValue()[2] != -1) {
                if (equipmentSlotEntry.getValue()[1] == -1 && equipmentSlotEntry.getValue()[2] < 9) {
/*					if (equipmentSlotEntry.getValue()[2] != mc.player.getInventory().selectedSlot) {
						mc.player.getInventory().selectedSlot = equipmentSlotEntry.getValue()[2];
						sendPacket(new UpdateSelectedSlotC2SPacket(equipmentSlotEntry.getValue()[2]));
					}*/
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + equipmentSlotEntry.getValue()[2], 1, SlotActionType.QUICK_MOVE, mc.player);
                    sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                } else if (mc.player.playerScreenHandler == mc.player.currentScreenHandler) {
                    int armorSlot = (equipmentSlotEntry.getValue()[0] - 34) + (39 - equipmentSlotEntry.getValue()[0]) * 2;
                    int newArmorSlot = equipmentSlotEntry.getValue()[2] < 9 ? 36 + equipmentSlotEntry.getValue()[2] : equipmentSlotEntry.getValue()[2];
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorSlot, 0, SlotActionType.PICKUP, mc.player);
                    if (equipmentSlotEntry.getValue()[1] != -1)
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                    sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                }
                return;
            }
        }
    }
    private int getProtection(ItemStack is) {
        if (isArmor(is) || is.getItem() == Items.ELYTRA) {
            int prot = 0;

            if (is.getItem() == Items.ELYTRA) {
                if (!isUsableElytra(is)) return 0;
                prot = 1;
            }
            if (is.hasEnchantments()) {
                ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(is);
                if (ignoreBinding.get() && com.dev.leavesHack.utils.entity.InventoryUtil.getEnchantmentLevel(is, Enchantments.BINDING_CURSE) > 0) return -1;
                prot += com.dev.leavesHack.utils.entity.InventoryUtil.getEnchantmentLevel(is, Enchantments.PROTECTION);
            }
            return getArmorProtection(is.getItem()) + prot;
        } else if (!is.isEmpty()) {
            return 0;
        }
        return -1;
    }

    private boolean isArmor(ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlot(stack);
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    private EquipmentSlot getEquipmentSlot(ItemStack stack) {
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        return equippable != null ? equippable.slot() : null;
    }

    private boolean isUsableElytra(ItemStack stack) {
        return stack.getItem() == Items.ELYTRA && (!stack.isDamageable() || stack.getDamage() < stack.getMaxDamage() - 1);
    }

    private int getArmorProtection(Item item) {
        if (item == Items.LEATHER_HELMET || item == Items.LEATHER_BOOTS || item == Items.GOLDEN_HELMET) return 1;
        if (item == Items.LEATHER_LEGGINGS || item == Items.CHAINMAIL_HELMET || item == Items.GOLDEN_BOOTS || item == Items.TURTLE_HELMET) return 2;
        if (item == Items.LEATHER_CHESTPLATE || item == Items.IRON_HELMET || item == Items.CHAINMAIL_BOOTS || item == Items.GOLDEN_LEGGINGS) return 3;
        if (item == Items.CHAINMAIL_LEGGINGS || item == Items.GOLDEN_CHESTPLATE || item == Items.IRON_BOOTS) return 4;
        if (item == Items.IRON_LEGGINGS || item == Items.DIAMOND_HELMET || item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_HELMET || item == Items.NETHERITE_BOOTS) return 5;
        if (item == Items.IRON_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS) return 6;
        if (item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE) return 8;
        return 0;
    }
}
