package de.pf2craft.item;

import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.Shield;
import de.pf2craft.core.character.Strike;
import de.pf2craft.core.item.ItemStamp;
import de.pf2craft.core.item.ItemVisual;
import de.pf2craft.core.item.RuneNames;
import de.pf2craft.core.item.WeaponData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Builds the vanilla item stacks that represent a character's weapons and shield. */
public final class Pf2ItemFactory {
    private Pf2ItemFactory() {
    }

    /** One stack per non-unarmed strike, plus a shield if the sheet has one. */
    public static List<ItemStack> create(UUID owner, CharacterSheet sheet) {
        long stamp = ItemStamp.of(sheet);
        List<ItemStack> out = new ArrayList<>();
        List<Strike> strikes = sheet.strikes();
        for (int i = 0; i < strikes.size(); i++) {
            Strike strike = strikes.get(i);
            if ("unarmed".equals(strike.category())) {
                continue;
            }
            ItemStack stack = new ItemStack(itemFor(strike));
            decorate(stack, displayName(strike), weaponLore(strike, sheet.name()));
            stack.set(Pf2Components.WEAPON, new WeaponRef(owner, i, stamp));
            out.add(stack);
        }
        sheet.shield().ifPresent(shield -> {
            ItemStack stack = new ItemStack(Items.SHIELD);
            decorate(stack, shield.name(), shieldLore(shield, sheet.name()));
            stack.set(Pf2Components.SHIELD, new ShieldRef(owner, stamp));
            out.add(stack);
        });
        return out;
    }

    /** "+1 Striking Rapier" style name from potency and striking runes. */
    static String displayName(Strike strike) {
        StringBuilder sb = new StringBuilder();
        if (strike.potency() > 0) {
            sb.append('+').append(strike.potency()).append(' ');
        }
        String striking = RuneNames.strikingWord(strike.damageDice());
        if (!striking.isEmpty()) {
            sb.append(striking).append(' ');
        }
        return sb.append(strike.name()).toString();
    }

    private static Item itemFor(Strike strike) {
        int range = ItemData.catalog().weapon(strike.name()).map(WeaponData::range).orElse(0);
        ItemVisual visual = ItemVisual.forWeapon(strike.damageDie(), strike.group(), range);
        ItemVisual.Material m = visual.material();
        return switch (visual.shape()) {
            case SWORD -> pick(m, Items.WOODEN_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
            case AXE -> pick(m, Items.WOODEN_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
            case PICKAXE -> pick(m, Items.WOODEN_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE,
                    Items.NETHERITE_PICKAXE);
            case SHOVEL -> pick(m, Items.WOODEN_SHOVEL, Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL,
                    Items.NETHERITE_SHOVEL);
            case HOE -> pick(m, Items.WOODEN_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
            case BOW -> Items.BOW;
            case CROSSBOW -> Items.CROSSBOW;
            case TRIDENT -> Items.TRIDENT;
            case STICK -> Items.STICK;
        };
    }

    private static Item pick(ItemVisual.Material m, Item wood, Item gold, Item diamond, Item netherite) {
        return switch (m) {
            case WOOD -> wood;
            case GOLD -> gold;
            case DIAMOND -> diamond;
            case NETHERITE -> netherite;
        };
    }

    private static void decorate(ItemStack stack, String name, List<Text> lore) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).styled(s -> s.withItalic(false)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        stack.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, stack
                .getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
                .withShowInTooltip(false));
    }

    private static List<Text> weaponLore(Strike strike, String ownerName) {
        List<Text> lore = new ArrayList<>();
        String die = strike.damageDie();
        String sides = die.startsWith("d") ? die.substring(1) : die;
        Text dice = Text.translatableWithFallback("pf2craft.items.dice", "%sd%s", strike.damageDice(), sides);
        lore.add(line(Text.translatableWithFallback("pf2craft.items.damage", "%s %s",
                dice, damageType(strike.damageType()))));
        if (!strike.traits().isEmpty()) {
            lore.add(line(Text.translatableWithFallback("pf2craft.items.traits", "Traits: %s",
                    String.join(", ", strike.traits()))));
        }
        if (!strike.propertyRunes().isEmpty()) {
            List<String> runes = strike.propertyRunes().stream().map(Pf2ItemFactory::titleCase).toList();
            lore.add(line(Text.translatableWithFallback("pf2craft.items.runes", "Runes: %s",
                    String.join(", ", runes))));
        }
        lore.add(line(Text.translatableWithFallback("pf2craft.items.owner", "Owner: %s", ownerName)));
        return lore;
    }

    private static List<Text> shieldLore(Shield shield, String ownerName) {
        return List.of(
                line(Text.translatableWithFallback("pf2craft.items.shield", "AC +%s, Hardness %s, HP %s",
                        shield.acBonus(), shield.hardness(), shield.hitPoints())),
                line(Text.translatableWithFallback("pf2craft.items.owner", "Owner: %s", ownerName)));
    }

    private static Text damageType(String type) {
        return switch (type) {
            case "B" -> Text.translatableWithFallback("pf2craft.damage.b", "bludgeoning");
            case "P" -> Text.translatableWithFallback("pf2craft.damage.p", "piercing");
            case "S" -> Text.translatableWithFallback("pf2craft.damage.s", "slashing");
            default -> Text.literal(type);
        };
    }

    private static Text line(MutableText text) {
        return text.styled(s -> s.withItalic(false).withColor(Formatting.GRAY));
    }

    private static String titleCase(String words) {
        StringBuilder sb = new StringBuilder();
        for (String part : words.split("\\s+")) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return sb.toString();
    }
}
