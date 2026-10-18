package com.magicbanana666.enderstorage.client;

import com.magicbanana666.enderstorage.EnderStorage;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * The ender chest: body, lid and a lock that turns into a diamond lock when the chest is personal and an emerald
 * lock when it belongs to a team.
 * Modelled upside down like the classic chest models (the renderer flips it).
 */
public class EnderChestModel extends Model<EnderChestModel.State> {

    public static final RenderType RENDER_TYPE = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(EnderStorage.MOD_ID, "textures/enderchest.png"));

    private final ModelPart lid;
    private final ModelPart lock;
    private final ModelPart diamondLock;
    private final ModelPart emeraldLock;

    public record State(float lidAngle, boolean owned, boolean team) {
    }

    public EnderChestModel() {
        super(createModel(), RenderTypes::entitySolid);
        this.lid = this.root.getChild("lid");
        this.lock = this.root.getChild("lock");
        this.diamondLock = this.root.getChild("diamond_lock");
        this.emeraldLock = this.root.getChild("emerald_lock");
    }

    private static ModelPart createModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(0F, 0F, 0F, 14, 10, 14), PartPose.offset(1F, 6F, 1F));
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(0F, -5F, -14F, 14, 5, 14), PartPose.offset(1F, 7F, 15F));
        root.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -2F, -15F, 2, 4, 1), PartPose.offset(8F, 7F, 15F));
        root.addOrReplaceChild("diamond_lock", CubeListBuilder.create().texOffs(0, 5).addBox(-1F, -2F, -15F, 2, 4, 1), PartPose.offset(8F, 7F, 15F));
        root.addOrReplaceChild("emerald_lock", CubeListBuilder.create().texOffs(6, 5).addBox(-1F, -2F, -15F, 2, 4, 1), PartPose.offset(8F, 7F, 15F));
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        this.lid.xRot = state.lidAngle();
        this.lock.xRot = state.lidAngle();
        this.diamondLock.xRot = state.lidAngle();
        this.emeraldLock.xRot = state.lidAngle();
        this.lock.visible = !state.owned() && !state.team();
        this.diamondLock.visible = state.owned();
        this.emeraldLock.visible = state.team();
    }
}
