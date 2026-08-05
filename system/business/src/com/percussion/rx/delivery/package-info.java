/**
 * The delivery system takes results from assembly, interprets the delivery type and
 * eventually delivers them to the appropriate destination. The assembly results may be
 * stored between the initial delivery and the eventual end of the publishing job.
 *
 * <p>A delivered item may either add or remove an item from the destination. When the job
 * is committed, the items to be removed (unpublished) are handled first. Then the items
 * to be replaced or added are handled.
 *
 * <h2>Status</h2>
 * As each item is delivered and when the items are committed, status is returned from the
 * handler. This status is passed along to the publishing results handler, which uses it to
 * signal the publishing system that the particular item has changed state, and whether the
 * delivery or committal is a success or failure. Failure messages are accumulated for the
 * given item in the item status table.
 *
 * <h2>Transactional Behavior</h2>
 * The use of the word transactional in this context does not refer to the standard ACID
 * criteria. Rather it really talks about the delivery handler buffering the published
 * changes to the end of the job, and applying those as an all or nothing batch. This
 * enables us to keep web sites more generally consistent, longer than if we were
 * outputting changes on a per-delivery mechanism.
 */
package com.percussion.rx.delivery;