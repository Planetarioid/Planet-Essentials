# Space Renderer GPU upload handoff

The CPU mesh pipeline must not call graphics APIs from worker threads. Prepared immutable meshes are handed to the render thread through a bounded, coalescing upload queue.

SpaceRendererGpuUploadQueue provides:
- FIFO batches constrained by both request count and byte budget.
- Per-section/material coalescing so a newer rebuild replaces an older queued upload.
- Hard entry and byte limits with rejection that preserves already queued work.
- Section-level removal for chunk unload and world invalidation.
- No graphics API ownership; the consumer must perform uploads and GPU resource retirement on the render thread.

This queue is an integration boundary, not a GPU uploader or terrain draw implementation. Actual device buffers, uploads, fences, and draw submission remain separate milestones.
