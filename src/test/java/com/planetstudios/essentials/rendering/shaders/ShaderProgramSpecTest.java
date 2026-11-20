package com.planetstudios.essentials.rendering.shaders;
import org.junit.jupiter.api.Test;import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ShaderProgramSpecTest{
 private ShaderProgramSpec spec(String v,String f){return new ShaderProgramSpec("planet_essentials:test/basic",Map.of(ShaderStage.VERTEX,v,ShaderStage.FRAGMENT,f));}
 @Test void hashIsStableAndContentSensitive(){assertEquals(spec("v","f").contentHash(),spec("v","f").contentHash());assertNotEquals(spec("v","f").contentHash(),spec("v2","f").contentHash());}
 @Test void rejectsBadIdAndMissingStages(){assertThrows(IllegalArgumentException.class,()->new ShaderProgramSpec("../bad",Map.of(ShaderStage.VERTEX,"v",ShaderStage.FRAGMENT,"f")));assertThrows(IllegalArgumentException.class,()->new ShaderProgramSpec("test:basic",Map.of(ShaderStage.VERTEX,"v")));}
 @Test void rejectsBlankAndOversizedSource(){assertThrows(IllegalArgumentException.class,()->spec(" ","f"));assertThrows(IllegalArgumentException.class,()->spec("x".repeat(ShaderProgramSpec.MAX_SOURCE_BYTES+1),"f"));}
 @Test void sourceMapIsImmutable(){assertThrows(UnsupportedOperationException.class,()->spec("v","f").sources().clear());}
}
