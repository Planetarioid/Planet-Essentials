package com.planetstudios.essentials.rendering.shaders;
/** Compilation/link failure reported by a concrete graphics backend. */
public final class ShaderCompileException extends Exception{
 private final String programId,diagnostics;
 public ShaderCompileException(String programId,String message,String diagnostics){
  super(message);this.programId=programId;this.diagnostics=diagnostics==null?"":diagnostics;
 }
 public String programId(){return programId;} public String diagnostics(){return diagnostics;}
}
