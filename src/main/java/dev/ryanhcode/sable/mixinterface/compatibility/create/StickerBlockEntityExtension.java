/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 */
package dev.ryanhcode.sable.mixinterface.compatibility.create;

import net.minecraft.core.HolderLookup;

public interface StickerBlockEntityExtension {
    public void sable$removeConstraint();

    public void sable$tickConstraint();

    public void sable$saveToContraption(HolderLookup.Provider var1);
}

