package org.patinanetwork.codebloom.common.db.repos.job;

import java.util.List;
import java.util.Optional;
import org.patinanetwork.codebloom.common.db.models.job.Job;

public interface JobRepository {
    /**
     * Creates a new job in the database.
     *
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param job - required fields:
     *     <ul>
     *       <li>questionId
     *       <li>status
     *     </ul>
     *     optional fields:
     *     <li>nextAttemptAt
     *         <ul>
     *     </ul>
     */
    void createJob(Job job);

    /**
     * Finds a job by its ID.
     *
     * @param id the job ID
     * @return the job if found, Optional.empty() otherwise
     */
    Optional<Job> findJobById(String id);

    /**
     * Returns all the jobs with the specified question id.
     *
     * @param id
     * @return A list of jobs that are joined to the question id.
     */
    List<Job> findJobsByQuestionId(String id);

    /**
     * Finds incomplete jobs ordered by nextAttemptAt (earliest first).
     *
     * @param maxJobs the maximum number of jobs to return
     * @return list of incomplete jobs ordered by earliest nextAttemptAt first
     */
    List<Job> findIncompleteJobs(int maxJobs);

    /**
     * Updates an existing job in the database.
     *
     * @note - The provided object's methods will be overridden with any returned data from the database.
     * @param job - required fields:
     *     <ul>
     *       <li>id
     *     </ul>
     *     updatable fields:
     *     <ul>
     *       <li>processedAt
     *       <li>completedAt
     *       <li>nextAttemptAt
     *       <li>status
     *       <li>attempts
     *     </ul>
     *
     * @return true if the update was successful, false otherwise
     */
    boolean updateJob(Job job);

    /**
     * Deletes a job by its ID.
     *
     * @param id the job ID to delete
     * @return true if the job was deleted successfully, false otherwise
     */
    boolean deleteJobById(String id);

    /**
     * Delete all jobs. <b>BE CAREFUL.</b>
     *
     * @return boolean indicating if all jobs were deleted successfully or not.
     */
    boolean deleteAllJobs();
}
