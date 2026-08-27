package com.hlysine.create_power_loader.content;

import com.hlysine.create_power_loader.config.CPLConfigs;
import com.hlysine.create_power_loader.content.trains.CPLGlobalStation;
import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.trains.station.StationBlockEntity;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static com.hlysine.create_power_loader.content.AbstractChunkLoaderBlock.ATTACHED;
import static com.hlysine.create_power_loader.content.ChunkLoadManager.LoadedChunkPos;
import static com.hlysine.create_power_loader.content.ChunkLoadManager.unforceAllChunks;
import static com.zurrtum.create.content.kinetics.base.DirectionalKineticBlock.FACING;

/**
 * The visual/analog-signal "active" state while attached to a station uses GlobalStation's own public
 * getPresentTrain(). The actual chunk-forcing for LoaderMode.STATION lives on {@link StationChunkLoader},
 * mixin-attached to GlobalStation itself rather than to this block entity - GlobalStation ticks through the
 * train graph, which runs independently of chunk loading, so it can force this loader's own chunk back open
 * even if it had gone fully cold, purely because a train arrived. See GlobalStationMixin/TrackEdgePointMixin.
 */
public abstract class AbstractChunkLoaderBlockEntity extends KineticBlockEntity implements ChunkLoader {

    public final LoaderType type;
    protected BlockPos lastBlockPos;
    protected boolean lastEnabled;
    protected int lastRange;
    protected int chunkUpdateCooldown;
    protected int chunkUnloadCooldown;
    protected Set<LoadedChunkPos> forcedChunks = new HashSet<>();
    @Nullable
    private StationBlockEntity attachedStation = null;
    public boolean isLoaderActive = false;

    public AbstractChunkLoaderBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state, LoaderType type) {
        super(typeIn, pos, state);
        this.type = type;
    }

    @Override
    public @NotNull Set<LoadedChunkPos> getForcedChunks() {
        return forcedChunks;
    }

    @Override
    public LoaderMode getLoaderMode() {
        return LoaderMode.STATIC;
    }

    @Override
    public LoaderType getLoaderType() {
        return type;
    }

    @Override
    public @Nullable Pair<Identifier, BlockPos> getLocation() {
        return Pair.of(getLevel().dimension().identifier(), getBlockPos());
    }

    public void updateAttachedStation(StationBlockEntity be) {
        if (attachedStation == null) {
            removeFromManager();
        } else if (attachedStation.getStation() instanceof CPLGlobalStation station) {
            station.getLoader().removeAttachment(getBlockPos());
        }
        attachedStation = be;
        if (attachedStation == null) {
            if (!level.isClientSide())
                addToManager();
        } else if (attachedStation.getStation() instanceof CPLGlobalStation station) {
            station.getLoader().addAttachment(type, getBlockPos(), getLoadingRange());
        }
    }

    /**
     * Re-registers this loader's current range with the attached station's loader, if any - called whenever
     * the range changes (e.g. the brass loader's scroll-wheel setting) so the station picks up the new value
     * immediately instead of waiting for a detach/reattach.
     */
    public void updateStationAttachment() {
        if (attachedStation != null && attachedStation.getStation() instanceof CPLGlobalStation station) {
            station.getLoader().addAttachment(type, getBlockPos(), getLoadingRange());
        }
    }

    public StationBlockEntity getAttachedStation() {
        return attachedStation;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (getLevel() != null && getBlockState().getValue(ATTACHED)) {
            BlockEntity be = getLevel().getBlockEntity(getBlockPos().relative(getBlockState().getValue(FACING).getOpposite()));
            if (!(be instanceof StationBlockEntity sbe)) return;
            updateAttachedStation(sbe);
        } else {
            if (!level.isClientSide())
                addToManager();
        }
    }

    public void reclaimChunks(Set<LoadedChunkPos> forcedChunks) {
        this.forcedChunks.addAll(forcedChunks);
    }

    @Override
    public void tick() {
        super.tick();

        boolean server = (!level.isClientSide() || isVirtual()) && (level instanceof ServerLevel);

        if (!server) {
            spawnParticles();
        }

        if (server) {
            boolean wasLoaderActive = isLoaderActive;
            isLoaderActive = CPLConfigs.server().getFor(type).enableStation.get() &&
                    attachedStation != null &&
                    attachedStation.getStation() != null &&
                    attachedStation.getStation().getPresentTrain() != null;
            if (wasLoaderActive != isLoaderActive) {
                notifyUpdate();
            }
        }

        if (server && chunkUpdateCooldown-- <= 0) {
            chunkUpdateCooldown = CPLConfigs.server().getFor(type).chunkUpdateInterval.get();
            if (needsUpdate()) {
                setChanged();
                updateForcedChunks();
            }
        }
    }

    private boolean needsUpdate() {
        if (lastBlockPos == null) return true;
        return !lastBlockPos.equals(getProjectedBlockPos()) || lastEnabled != canLoadChunks() || lastRange != getLoadingRange() || chunkUnloadCooldown > 0;
    }

    protected void updateForcedChunks() {
        boolean resetStates = true;
        if (canLoadChunks()) {
            ChunkLoadManager.updateForcedChunks(
                    (ServerLevel) level,
                    new ChunkLoadManager.DimensionalBlockPos(getLevel().dimension().identifier(), getBlockPos()),
                    getBlockPos(),
                    getLoadingRange(),
                    forcedChunks
            );
        } else if (chunkUnloadCooldown >= CPLConfigs.server().getFor(type).unloadGracePeriod.get()) {
            unforceAllChunks(level.getServer(), getBlockPos(), forcedChunks);
        } else {
            chunkUnloadCooldown += CPLConfigs.server().getFor(type).chunkUpdateInterval.get();
            resetStates = false;
        }
        if (resetStates) {
            chunkUnloadCooldown = 0;
            lastBlockPos = getProjectedBlockPos();
            lastEnabled = canLoadChunks();
            lastRange = getLoadingRange();
        }
    }

    public boolean canLoadChunks() {
        return isSpeedRequirementFulfilled() && CPLConfigs.server().getFor(type).enableStatic.get();
    }

    @Override
    public boolean isSpeedRequirementFulfilled() {
        if (!super.isSpeedRequirementFulfilled())
            return false;

        BlockState state = getBlockState();
        if (!(getBlockState().getBlock() instanceof IRotate))
            return true;
        IRotate def = (IRotate) state.getBlock();
        IRotate.SpeedLevel minimumRequiredSpeedLevel = def.getMinimumRequiredSpeedLevel();
        float minSpeed = minimumRequiredSpeedLevel.getSpeedValue();

        double requirement = minSpeed * (float) Math.pow(2, getLoadingRange()) * CPLConfigs.server().getFor(type).speedMultiplier.get();
        return Math.abs(getSpeed()) >= requirement;
    }

    @Override
    public void destroy() {
        super.destroy();
        boolean server = (!level.isClientSide() || isVirtual()) && (level instanceof ServerLevel);
        if (server)
            unforceAllChunks(level.getServer(), getBlockPos(), forcedChunks);
        updateAttachedStation(null);
        removeFromManager();
    }

    @Override
    public void remove() {
        super.remove();
        boolean server = (!level.isClientSide() || isVirtual()) && (level instanceof ServerLevel);
        if (server)
            unforceAllChunks(level.getServer(), getBlockPos(), forcedChunks);
        updateAttachedStation(null);
        removeFromManager();
    }

    @Override
    protected void read(ValueInput view, boolean clientPacket) {
        super.read(view, clientPacket);
        isLoaderActive = view.getBooleanOr("CoreActive", false);
    }

    @Override
    protected void write(ValueOutput view, boolean clientPacket) {
        view.putBoolean("CoreActive", isLoaderActive);
        super.write(view, clientPacket);
    }

    public abstract int getLoadingRange();

    protected void spawnParticles() {
        if (level == null)
            return;
        if (!canLoadChunks())
            return;

        RandomSource r = level.getRandom();

        Vec3 c = VecHelper.getCenterOf(worldPosition);

        if (r.nextInt(4) != 0)
            return;

        double speed = .0625f;
        Vec3 normal = Vec3.atLowerCornerOf(getBlockState().getValue(BlockStateProperties.FACING).getUnitVec3i());
        Vec3 v2 = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .5f)
                        .multiply(1, 1, 1)
                        .normalize()
                        .scale((.25f) + r.nextDouble() * .125f))
                .add(normal.scale(0.5f));

        Vec3 motion = normal.scale(speed);
        level.addParticle(ParticleTypes.PORTAL, v2.x, v2.y, v2.z, motion.x, motion.y, motion.z);
    }

    protected BlockPos getProjectedBlockPos() {
        return getBlockPos();
    }
}
