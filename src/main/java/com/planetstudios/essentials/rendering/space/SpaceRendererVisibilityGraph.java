package com.planetstudios.essentials.rendering.space;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Conservative section-connectivity traversal; unknown graph edges require frustum fallback. */
public final class SpaceRendererVisibilityGraph {
    public static final int NEGATIVE_X=1, POSITIVE_X=1<<1, NEGATIVE_Y=1<<2,
            POSITIVE_Y=1<<3, NEGATIVE_Z=1<<4, POSITIVE_Z=1<<5, ALL_FACES=(1<<6)-1;
    private static final Direction[] DIRECTIONS={
            new Direction(-1,0,0,NEGATIVE_X,POSITIVE_X),new Direction(1,0,0,POSITIVE_X,NEGATIVE_X),
            new Direction(0,-1,0,NEGATIVE_Y,POSITIVE_Y),new Direction(0,1,0,POSITIVE_Y,NEGATIVE_Y),
            new Direction(0,0,-1,NEGATIVE_Z,POSITIVE_Z),new Direction(0,0,1,POSITIVE_Z,NEGATIVE_Z)};
    private final Map<SpaceRendererSectionManager.SectionKey,Integer> connectivity=new HashMap<>();
    public synchronized void update(SpaceRendererSectionManager.SectionKey key,int openFaces){
        Objects.requireNonNull(key,"key");
        if((openFaces&~ALL_FACES)!=0) throw new IllegalArgumentException("Unknown face bits");
        connectivity.put(key,openFaces);
    }
    public synchronized boolean remove(SpaceRendererSectionManager.SectionKey key){return connectivity.remove(Objects.requireNonNull(key,"key"))!=null;}
    public synchronized int removeDimension(String dimension){
        Objects.requireNonNull(dimension,"dimension"); int removed=0;
        var iterator=connectivity.keySet().iterator();
        while(iterator.hasNext()) if(iterator.next().dimensionId().equals(dimension)){iterator.remove();removed++;}
        return removed;
    }
    public synchronized VisibilityResult visibleFrom(SpaceRendererSectionManager.SectionKey origin,int maximumVisited){
        Objects.requireNonNull(origin,"origin");
        if(maximumVisited<=0) throw new IllegalArgumentException("Visit limit must be positive");
        if(!connectivity.containsKey(origin)) return new VisibilityResult(Set.of(),true,0);
        Set<SpaceRendererSectionManager.SectionKey> visible=new HashSet<>();
        ArrayDeque<SpaceRendererSectionManager.SectionKey> queue=new ArrayDeque<>();
        visible.add(origin);queue.add(origin);boolean incomplete=false;
        while(!queue.isEmpty()){
            if(visible.size()>=maximumVisited){incomplete=true;break;}
            var current=queue.removeFirst();int mask=connectivity.get(current);
            for(Direction direction:DIRECTIONS){
                if((mask&direction.face())==0) continue;
                var neighbor=new SpaceRendererSectionManager.SectionKey(current.dimensionId(),
                        current.x()+direction.dx(),current.y()+direction.dy(),current.z()+direction.dz());
                Integer neighborMask=connectivity.get(neighbor);
                if(neighborMask==null){incomplete=true;continue;}
                if((neighborMask&direction.opposite())!=0&&visible.add(neighbor)) queue.addLast(neighbor);
            }
        }
        return new VisibilityResult(Set.copyOf(visible),incomplete,visible.size());
    }
    public synchronized int trackedSectionCount(){return connectivity.size();}
    public record VisibilityResult(Set<SpaceRendererSectionManager.SectionKey> visibleSections,
            boolean requiresFrustumFallback,int visitedSectionCount){
        public VisibilityResult{
            visibleSections=Set.copyOf(Objects.requireNonNull(visibleSections,"visibleSections"));
            if(visitedSectionCount!=visibleSections.size()) throw new IllegalArgumentException("Visited count mismatch");
        }
        public boolean isComplete(){return !requiresFrustumFallback;}
    }
    private record Direction(int dx,int dy,int dz,int face,int opposite){}
}
