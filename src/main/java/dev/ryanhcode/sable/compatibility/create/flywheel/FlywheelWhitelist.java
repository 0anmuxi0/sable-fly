package dev.ryanhcode.sable.compatibility.create.flywheel;

import com.zurrtum.create.AllBlockEntityTypes;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Phase-1 whitelist for the Flywheel embedding adapter: only the listed BE types are
 * injected into the parent-world manager (queueAdd) by {@link FlywheelCompat}.
 *
 * <p>Motivation: enabling the adapter for every type made some physics bodies render
 * less content than before, because BEs previously drawn by Sable's own extraction
 * pipeline were taken over by flywheel, whose embedding pipeline does not cover every
 * visual shape yet. The whitelist restores the core kinetic parts (gears/shafts
 * spinning) first; all other types keep using the original pipeline.</p>
 *
 * <p>26.1.2 port: initialization is lazy instead of an explicit client-init hook.
 * Sable does not depend on Create, so its client entrypoint runs before Create's
 * registries are populated; the first {@link #contains} call only happens from the
 * guarded mixins (Create installed, a Create block entity being classified), at which
 * point {@code AllBlockEntityTypes} is safe to touch.</p>
 */
public final class FlywheelWhitelist {
    private FlywheelWhitelist() {
    }

    /** Filled lazily on first {@link #contains} call (after Create registries are ready). */
    private static final Set<BlockEntityType<?>> TYPES = new HashSet<>();

    private static boolean initialized;

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        // Kinetic core: bracketed shaft / motor / rotation speed controller / encased
        // shaft / powered shaft / gearbox.
        add(AllBlockEntityTypes.BRACKETED_KINETIC,
                AllBlockEntityTypes.MOTOR,
                AllBlockEntityTypes.ROTATION_SPEED_CONTROLLER,
                AllBlockEntityTypes.ENCASED_SHAFT,
                AllBlockEntityTypes.POWERED_SHAFT,
                AllBlockEntityTypes.GEARBOX);
        // Clutch and chain-transmission gearboxes.
        add(AllBlockEntityTypes.ADJUSTABLE_CHAIN_GEARSHIFT,
                AllBlockEntityTypes.CLUTCH,
                AllBlockEntityTypes.GEARSHIFT,
                AllBlockEntityTypes.SEQUENCED_GEARSHIFT);
        // Piston / encased cogwheels / hand crank / valve handle / turntable.
        add(AllBlockEntityTypes.MECHANICAL_PISTON,
                AllBlockEntityTypes.ENCASED_COGWHEEL,
                AllBlockEntityTypes.ENCASED_LARGE_COGWHEEL,
                AllBlockEntityTypes.HAND_CRANK,
                AllBlockEntityTypes.VALVE_HANDLE,
                AllBlockEntityTypes.TURNTABLE);
        // Processing machines.
        add(AllBlockEntityTypes.DRILL,
                AllBlockEntityTypes.MILLSTONE,
                AllBlockEntityTypes.MECHANICAL_MIXER,
                AllBlockEntityTypes.CRUSHING_WHEEL);
        // Fluids.
        add(AllBlockEntityTypes.MECHANICAL_PUMP,
                AllBlockEntityTypes.GLASS_FLUID_PIPE,
                AllBlockEntityTypes.FLUID_VALVE,
                AllBlockEntityTypes.STEAM_ENGINE);
        // Gantries.
        add(AllBlockEntityTypes.GANTRY_SHAFT,
                AllBlockEntityTypes.GANTRY_PINION);
        // Gauges.
        add(AllBlockEntityTypes.SPEEDOMETER,
                AllBlockEntityTypes.STRESSOMETER);
        // Diodes / levers / sticker.
        add(AllBlockEntityTypes.PULSE_REPEATER,
                AllBlockEntityTypes.PULSE_EXTENDER,
                AllBlockEntityTypes.PULSE_TIMER,
                AllBlockEntityTypes.ANALOG_LEVER,
                AllBlockEntityTypes.STICKER);
        // Kinetic flywheel (the Create block, not the renderer library).
        add(AllBlockEntityTypes.FLYWHEEL);
    }

    private static void add(BlockEntityType<?>... types) {
        for (BlockEntityType<?> type : types) {
            if (type != null) {
                TYPES.add(type);
            }
        }
    }

    public static boolean contains(BlockEntityType<?> type) {
        if (!initialized) {
            init();
        }
        return TYPES.contains(type);
    }
}
