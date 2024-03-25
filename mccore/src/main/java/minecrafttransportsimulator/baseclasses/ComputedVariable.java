package minecrafttransportsimulator.baseclasses;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;

/**
 * A helper class of sorts for calculating computed variables.  This helper class wraps up a set of functions
 * for obtaining the variable value based on the various constructor arguments.  In more specific
 * terms, it parses the string-saved information and can return a specific value for this information.
 * The value to be provided is based on a functional interface provided to the object, the implementation
 * of this being left up to however this implementation is structured.
 *
 * @author don_bruce
 */
public class ComputedVariable {
    public static final String INVERTED_PREFIX = "!";
    public static final String CONSTANT_PREFIX = "#";
    public static final ComputedVariable ZERO_VARIABLE = new ComputedVariable(null, "#0", null);
    public static final ComputedVariable ONE_VARIABLE = new ComputedVariable(null, "#1", null);

	/**The key of this variable, required to be unique to all variables on the entity.**/
    public final String variableKey;
    /**The entity this variable is defined on.**/
    public final AEntityD_Definable<?> entity;
    private final ComputedVariableOperator function;
    private final boolean changesOnPartialTicks;
    private final boolean randomVariable;
    private final boolean isConstant;
    private long lastTickChecked;
    /**The current value of this variable.  Only change by calling one of the functions in this class.**/
    public double currentValue;
    /**True if {@link #currentValue} is greater than 1, false otherwise.  Used for quicker boolean operations.**/
    public boolean isActive;
    /**Flag for external systems to allow resetting this CV.  Doesn't do any logic, but external systems can use this for state-resets.
     * The reason this flag is here is so if this CV is referenced on multiple entities, each can do their own resetting logic.
     * Only set by calling {@link #reset()}, never by manually setting the flag.**/
    public boolean needsReset;
    /**Internal variable for the inverted state of this variable.  Is read-only since we just set its states when ours change.
     * Is null on the inverted variable itself.**/
    public final ComputedVariable invertedVariable;

    /**Constructor for variables with logic that can change each tick or frame.**/
    public ComputedVariable(AEntityD_Definable<?> entity, String variable, ComputedVariableOperator function, boolean changesOnPartialTicks) {
        this.function = function;
        this.variableKey = variable;
        this.entity = entity;
        this.changesOnPartialTicks = changesOnPartialTicks;
        this.randomVariable = variable.startsWith("random");
        this.isConstant = variable.startsWith(CONSTANT_PREFIX);
        this.invertedVariable = variable.startsWith(INVERTED_PREFIX) ? null : new ComputedVariable(entity, INVERTED_PREFIX + variable, null);
        if (invertedVariable != null) {
            entity.addVariable(invertedVariable);
        }
    }

    /**Constructor for variables with no logic, and instead maintained state.**/
    public ComputedVariable(AEntityD_Definable<?> entity, String variable, IWrapperNBT data) {
        this(entity, variable, null, false);
        if (variable.startsWith(CONSTANT_PREFIX)) {
            setInternal(Double.parseDouble(variable.substring(CONSTANT_PREFIX.length())));
        }else if(data != null) {
            setInternal(data.getDouble(variableKey));
        }
    }

    @Override
    public String toString() {
        return variableKey + ":" + currentValue;
    }

    public static boolean isNumberedVariable(String variable) {
        return variable.matches("^.*_\\d+$");
    }

    /**
     * Helper method to get the index of the passed-in variable.  Indexes are defined by
     * variable names ending in _xx, where xx is a number.  The defined number is assumed
     * to be 1-indexed (JSON), but the returned number will be 0-indexed (Lists).  
     * When calling this method, ensure the variable defines a number, preferably by calling
     * {@link #isNumberedVariable(String)} before, otherwise this will cause a formatting crash.
     */
    public static int getVariableNumber(String variable) {
        return Integer.parseInt(variable.substring(variable.lastIndexOf('_') + 1)) - 1;
    }

    /**Helper to set the value, does other functions as well to maintain state.**/
    private final void setInternal(double value) {
        currentValue = value;
        isActive = currentValue > 0;
        if (invertedVariable != null) {
            invertedVariable.currentValue = currentValue > 0 ? 0 : 1;
            invertedVariable.isActive = !this.isActive;
        }
    }

    public final double getValue() {
        return computeValue(0);
    }

    /**
     * Computes the value of this variable, updating {@link #currentValue}, and returning it.
     */
    public final double computeValue(float partialTicks) {
        if (function != null) {
            if (randomVariable || (changesOnPartialTicks && partialTicks != 0)) {
                setInternal(function.apply(partialTicks));
            } else if (lastTickChecked != entity.ticksExisted) {
                setInternal(function.apply(partialTicks));
                lastTickChecked = entity.ticksExisted;
            }
        }
        return currentValue;
    }

    public final void setTo(double value, boolean sendPacket) {
        if (!isConstant) {
            setInternal(value);
            if (sendPacket) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(this, currentValue));
            }
        }
    }
    
    public final void adjustBy(double value, boolean sendPacket) {
        if (!isConstant) {
            setInternal(currentValue + value);
            if (sendPacket) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, value));
            }
        }
    }

    public final void toggle(boolean sendPacket) {
        if (!isConstant) {
            setInternal(currentValue > 0 ? 0 : 1);
            if (sendPacket) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableToggle(this));
            }
        }
    }

    /**
     * Returns true if the value was changed, false if not due to clamps.
     */
    public final boolean increment(double incrementValue, double minValue, double maxValue, boolean sendPacket) {
        if (!isConstant) {
            double newValue = currentValue + incrementValue;
            if (minValue != 0 || maxValue != 0) {
                if (newValue < minValue) {
                    newValue = minValue;
                } else if (newValue > maxValue) {
                    newValue = maxValue;
                }
            }
            if (newValue != currentValue) {
                incrementValue = newValue - currentValue;
                setInternal(newValue);
                if (sendPacket) {
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, incrementValue, minValue, maxValue));
                }
                return true;
            }
        }
        return false;
    }

    public final void reset() {
        if (!isConstant) {
            needsReset = true;
        }
    }

    public final void saveToNBT(List<String> savedNames, IWrapperNBT data) {
        if (function == null) {
            data.setDouble(variableKey, currentValue);
            savedNames.add(variableKey);
        }
    }

    @FunctionalInterface
    public interface ComputedVariableOperator {
        double apply(float operand);
    }
}
