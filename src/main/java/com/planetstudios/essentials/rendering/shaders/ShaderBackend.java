package com.planetstudios.essentials.rendering.shaders;
/**
 * Backend SPI. Implementations compile on the graphics context's required thread,
 * own native handles, and release all owned resources from CompiledProgram.close().
 */
public interface ShaderBackend{
 String name(); ShaderCapabilities capabilities();
 CompiledProgram compile(ShaderProgramSpec spec)throws ShaderCompileException;
 interface CompiledProgram extends AutoCloseable{
  String programId();String sourceHash();@Override void close();
 }
}
