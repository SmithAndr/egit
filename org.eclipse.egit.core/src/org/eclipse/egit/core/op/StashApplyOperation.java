/******************************************************************************
 *  Copyright (c) 2012, 2015 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Laurent Delaigue (Obeo) - use of preferred merge strategy
 *****************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.StashApplyCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

/**
 * Operation that applies a stashed commit in a repository
 */
public class StashApplyOperation implements IEGitOperation {

	private final Repository repository;

	private final RevCommit commit;

	/**
	 * Create operation for repository
	 *
	 * @param repository
	 * @param commit
	 */
	public StashApplyOperation(final Repository repository,
			final RevCommit commit) {
		this.repository = repository;
		this.commit = commit;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor pm) throws CoreException {
				pm.beginTask("", 3); //$NON-NLS-1$
				try {
					IProject[] validProjects = ProjectUtil
							.getValidOpenProjects(repository);
					pm.worked(1);
					StashApplyCommand command = Git.wrap(repository)
							.stashApply().setStashRef(commit.name());
					MergeStrategy strategy = Activator.getDefault()
							.getPreferredMergeStrategy();
					if (strategy != null) {
						command.setStrategy(strategy);
					}
					command.call();
					pm.worked(1);
					ProjectUtil.refreshValidProjects(validProjects,
							new SubProgressMonitor(pm, 1));
				} catch (JGitInternalException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(),
							e.getCause());
				} finally {
					pm.done();
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE,
				monitor != null ? monitor : new NullProgressMonitor());
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repository);
	}
}
