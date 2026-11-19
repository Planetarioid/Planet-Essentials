package com.planetstudios.essentials.rendering.shaders;
import org.junit.jupiter.api.Test;import java.util.List;import java.util.Map;import java.util.Set;import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class ShaderPipelineTest{
 private static ShaderProgramSpec spec(String id){return new ShaderProgramSpec("planet_essentials:"+id,Map.of(ShaderStage.VERTEX,"v-"+id,ShaderStage.FRAGMENT,"f-"+id));}
 private static final class FakeBackend implements ShaderBackend{
  final AtomicInteger closed=new AtomicInteger();String failOn;
  public String name(){return "test-backend";}
  public ShaderCapabilities capabilities(){return new ShaderCapabilities(name(),4096,4,16,Set.of(),Set.of("basic"));}
  public CompiledProgram compile(ShaderProgramSpec s)throws ShaderCompileException{
   if(failOn!=null&&s.id().endsWith(failOn))throw new ShaderCompileException(s.id(),"synthetic failure","test diagnostic");
   return new CompiledProgram(){public String programId(){return s.id();}public String sourceHash(){return s.contentHash();}public void close(){closed.incrementAndGet();}};
  }
 }
 @Test void publishesGenerationAndReleasesPrevious(){ShaderPipeline p=new ShaderPipeline();FakeBackend b=new FakeBackend();assertTrue(p.reload(List.of(spec("one"),spec("two")),b));assertEquals(2,p.snapshot().programCount());assertTrue(p.reload(List.of(spec("three")),b));assertEquals(2,b.closed.get());p.close();assertEquals(3,b.closed.get());}
 @Test void failedReloadKeepsPreviousAndClosesCandidates(){ShaderPipeline p=new ShaderPipeline();FakeBackend b=new FakeBackend();assertTrue(p.reload(List.of(spec("stable")),b));b.failOn="broken";assertFalse(p.reload(List.of(spec("candidate"),spec("broken")),b));assertNotNull(p.program("planet_essentials:stable"));assertEquals(1,p.snapshot().programCount());assertTrue(p.snapshot().lastFailure().contains("synthetic failure"));assertEquals(1,b.closed.get());p.close();}
 @Test void duplicateIdsDoNotReplaceActivePrograms(){ShaderPipeline p=new ShaderPipeline();FakeBackend b=new FakeBackend();assertTrue(p.reload(List.of(spec("stable")),b));assertFalse(p.reload(List.of(spec("duplicate"),spec("duplicate")),b));assertNotNull(p.program("planet_essentials:stable"));p.close();}
 @Test void invalidationReleasesProgramsAndClosePreventsReload(){ShaderPipeline p=new ShaderPipeline();FakeBackend b=new FakeBackend();assertTrue(p.reload(List.of(spec("one")),b));p.invalidate();assertEquals(0,p.snapshot().programCount());assertEquals(1,b.closed.get());p.close();assertThrows(IllegalStateException.class,()->p.reload(List.of(),b));}
}
