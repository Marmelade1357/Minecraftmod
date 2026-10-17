package de.pf2craft.core.item;

/** Which Minecraft-style tool shape and material tier best represents a weapon. */
public record ItemVisual(Shape shape, Material material) {
    public enum Shape { SWORD, AXE, PICKAXE, SHOVEL, HOE, BOW, CROSSBOW, TRIDENT, STICK }

    public enum Material { WOOD, GOLD, DIAMOND, NETHERITE }

    /** {@code range} is in feet, 0 for melee weapons. */
    public static ItemVisual forWeapon(String die, String group, int range) {
        Material material = switch (die) {
            case "d6" -> Material.GOLD;
            case "d8" -> Material.DIAMOND;
            case "d10", "d12" -> Material.NETHERITE;
            default -> Material.WOOD;
        };
        Shape shape = switch (group) {
            case "sword", "knife" -> Shape.SWORD;
            case "axe" -> Shape.AXE;
            case "pick" -> Shape.PICKAXE;
            case "polearm", "spear" -> Shape.TRIDENT;
            case "bow" -> Shape.BOW;
            case "crossbow", "firearm" -> Shape.CROSSBOW;
            case "club", "hammer", "flail", "brawling" -> Shape.STICK;
            default -> range == 0 ? Shape.SWORD : Shape.BOW;
        };
        return new ItemVisual(shape, material);
    }
}
