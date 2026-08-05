/**
 * The publishing system takes a unit of work to publish, called an edition, and executes
 * the edition. The edition consists of a series of content lists, each of which identify
 * zero or more content items to be assembled using specified templates and placed in
 * calculated locations. The edition also references a site, which defines where
 * filesystem content will be published, as well as identifying other per-site information
 * to be used in assembly and delivery.
 *
 * <h2>The Publishing Job</h2>
 * The central concept in publishing is the job. The job can be thought of as an
 * instantiation of the edition. The job reads the site and edition data, runs pre- and
 * post-edition tasks, and manages the assembly and delivery of the content.
 *
 * <p>The job sends the content to be assembled to the publishing queue. A configured set
 * of processors take the assembly items and produce delivery results. The delivery
 * results are sent to the delivery manager, which invokes the appropriate delivery
 * handler. The handler then either stores the result for later delivery when the job
 * "commits", or delivers right away for old-style {@code IPSPublisher} handlers.
 *
 * <p>Each job is identified by a job id. The id is a 64 bit number. This number is
 * incremented for each job run and will never, in a practical sense, repeat. Each item
 * is identified by a reference id, which are also 64 bit numbers. The job id is used
 * for the status table and the reference id is used for the items table. Both tables
 * are periodically purged of old content.
 *
 * <h2>Publishing Handler</h2>
 * The publishing handler accepts work to be done from the publishing queue. The handler
 * first uses the assembly service to create an assembly result and then calls the
 * delivery manager to deliver the content. It sends status to the publishing results
 * handler via the result queue.
 *
 * <h2>Publishing Results Handler</h2>
 * The publishing results handler takes status updates and forwards them to the business
 * publisher service, which maintains the job state and updates the database via the
 * publisher service.
 */
package com.percussion.rx.publisher;