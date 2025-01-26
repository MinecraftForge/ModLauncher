/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import cpw.mods.modlauncher.api.ServiceRunner;

import java.lang.invoke.*;

/**
 * Test harness launch service - this will do nothing, but will take "test.harness" and offer it to the transformer
 * system. Should be ideal for testing external transformers.
 */
public class TestingLaunchHandlerService implements ILaunchHandlerService {
    @Override
    public String name() {
        return "testharness";
    }

    public void launchService(String[] arguments, ModuleLayer gameLayer) {
        try {
            Class<?> callableLaunch = Class.forName(System.getProperty("test.harness.callable"), true, Thread.currentThread().getContextClassLoader());
            getClass().getModule().addReads(callableLaunch.getModule());
            MethodHandle handle = MethodHandles.lookup().findStatic(callableLaunch, "supplier", MethodType.methodType(ServiceRunner.class));
            handle.invokeExact();
        } catch (ClassNotFoundException | NoSuchMethodException | LambdaConversionException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }
}
