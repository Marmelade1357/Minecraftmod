package de.pf2craft.network;

import de.pf2craft.character.CharacterService;
import de.pf2craft.character.SheetService;
import de.pf2craft.combat.ActionService;
import de.pf2craft.combat.AttackService;
import de.pf2craft.combat.CombatService;
import de.pf2craft.combat.ConditionService;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.npc.NpcControl;
import de.pf2craft.npc.NpcNetwork;
import de.pf2craft.roll.RollService;
import de.pf2craft.spell.SpellService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Pf2Networking {
    private Pf2Networking() {
    }

    /** Runs on both sides, the payload types must be known to client and server. */
    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(RollRequestPayload.ID, RollRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RollResultPayload.ID, RollResultPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ImportCharacterPayload.ID, ImportCharacterPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenImportPayload.ID, OpenImportPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SheetRequestPayload.ID, SheetRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SheetSyncPayload.ID, SheetSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SpellbookSyncPayload.ID, SpellbookSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CastSpellPayload.ID, CastSpellPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SpellTargetPayload.ID, SpellTargetPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CheckRollPayload.ID, CheckRollPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GmEditPayload.ID, GmEditPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GrantLevelUpPayload.ID, GrantLevelUpPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LevelUpPayload.ID, LevelUpPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NpcSearchPayload.ID, NpcSearchPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NpcSearchResultPayload.ID, NpcSearchResultPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NpcSpawnPayload.ID, NpcSpawnPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NpcSheetPayload.ID, NpcSheetPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NpcEditPayload.ID, NpcEditPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NpcActionPayload.ID, NpcActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenNpcScreenPayload.ID, OpenNpcScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CombatSyncPayload.ID, CombatSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CombatStartPayload.ID, CombatStartPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CombatControlPayload.ID, CombatControlPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CombatRemovePayload.ID, CombatRemovePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EndTurnPayload.ID, EndTurnPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenCombatScreenPayload.ID, OpenCombatScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenAttackScreenPayload.ID, OpenAttackScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AttackConfirmPayload.ID, AttackConfirmPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ActionRequestPayload.ID, ActionRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PendingActionPayload.ID, PendingActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ConditionEditPayload.ID, ConditionEditPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TakeOverPayload.ID, TakeOverPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ControlInputPayload.ID, ControlInputPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ControlActionPayload.ID, ControlActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ControlStatePayload.ID, ControlStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NpcSpeechPayload.ID, NpcSpeechPayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ImportCharacterPayload.ID,
                (payload, context) -> CharacterService.startImport(context.player(), payload.pathbuilderId()));
        ServerPlayNetworking.registerGlobalReceiver(SheetRequestPayload.ID,
                (payload, context) -> SheetService.request(context.player(), payload.target()));
        ServerPlayNetworking.registerGlobalReceiver(CastSpellPayload.ID,
                (payload, context) -> SpellService.cast(context.player(), payload.entry(), payload.spell(),
                        payload.rank()));
        ServerPlayNetworking.registerGlobalReceiver(SpellTargetPayload.ID,
                (payload, context) -> SpellService.target(context.player(), payload.entityId()));
        ServerPlayNetworking.registerGlobalReceiver(CheckRollPayload.ID,
                (payload, context) -> SheetService.rollCheck(context.player(), payload.checkId(), payload.secret()));
        ServerPlayNetworking.registerGlobalReceiver(GmEditPayload.ID,
                (payload, context) -> SheetService.gmEdit(context.player(), payload.target(), payload.field(),
                        payload.value()));
        ServerPlayNetworking.registerGlobalReceiver(GrantLevelUpPayload.ID,
                (payload, context) -> SheetService.grantLevelUp(context.player(), payload.target()));
        ServerPlayNetworking.registerGlobalReceiver(LevelUpPayload.ID,
                (payload, context) -> SheetService.levelUp(context.player(), payload.skillIncrease(),
                        payload.boosts(), payload.featNames()));
        ServerPlayNetworking.registerGlobalReceiver(NpcSearchPayload.ID,
                (payload, context) -> NpcNetwork.search(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(NpcSpawnPayload.ID,
                (payload, context) -> NpcNetwork.spawn(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(NpcEditPayload.ID,
                (payload, context) -> NpcNetwork.edit(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(NpcActionPayload.ID,
                (payload, context) -> NpcNetwork.action(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(CombatStartPayload.ID,
                (payload, context) -> CombatService.startFromGm(context.player(), payload.participants()));
        ServerPlayNetworking.registerGlobalReceiver(CombatControlPayload.ID,
                (payload, context) -> CombatService.control(context.player(), payload.action()));
        ServerPlayNetworking.registerGlobalReceiver(CombatRemovePayload.ID,
                (payload, context) -> CombatService.removeFromGm(context.player(), payload.id()));
        ServerPlayNetworking.registerGlobalReceiver(EndTurnPayload.ID,
                (payload, context) -> CombatService.endTurn(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(AttackConfirmPayload.ID,
                (payload, context) -> AttackService.confirm(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(ActionRequestPayload.ID,
                (payload, context) -> ActionService.request(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(ConditionEditPayload.ID,
                (payload, context) -> ConditionService.editFromClient(context.player(), payload.target(),
                        payload.conditionId(), payload.value()));
        ServerPlayNetworking.registerGlobalReceiver(TakeOverPayload.ID,
                (payload, context) -> NpcControl.request(context.player(), payload.take()));
        ServerPlayNetworking.registerGlobalReceiver(ControlInputPayload.ID,
                (payload, context) -> NpcControl.input(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(ControlActionPayload.ID,
                (payload, context) -> NpcControl.action(context.player(), payload.kind()));
        ServerPlayNetworking.registerGlobalReceiver(RollRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!RollService.checkCooldown(player)) {
                return;
            }
            try {
                RollService.roll(player.getServer(), player, player.getName().getString(),
                        payload.expression(), payload.label(), payload.secret());
            } catch (DiceParseException e) {
                player.sendMessage(Text.translatableWithFallback("pf2craft.roll.invalid",
                        "Invalid dice expression: %s", e.getMessage()).formatted(Formatting.RED), false);
            }
        });
    }
}
