package amanide.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

/**
 * @author Fabio Zadrozny
 */
public class JobProgressComunicator implements IProgressMonitor {

	private IProgressMonitor monitor;
	private Job job;

	public JobProgressComunicator(IProgressMonitor monitor, String main,
			int total, Job job) {
		this.monitor = monitor;
		this.job = job;
		this.monitor.beginTask(main, total);
	}

	@Override
	public void done() {
		monitor.done();
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#beginTask(java.lang.String,
	 *      int)
	 */
	@Override
	public void beginTask(String name, int totalWork) {
		this.monitor.beginTask(name, totalWork);
		this.job.setName(name);
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#internalWorked(double)
	 */
	@Override
	public void internalWorked(double work) {
		this.monitor.internalWorked(work);
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#isCanceled()
	 */
	@Override
	public boolean isCanceled() {
		return this.monitor.isCanceled();
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#setCanceled(boolean)
	 */
	@Override
	public void setCanceled(boolean value) {
		this.monitor.setCanceled(value);
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#setTaskName(java.lang.String)
	 */
	@Override
	public void setTaskName(String name) {
		this.monitor.setTaskName(name);
		this.job.setName(name);
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#subTask(java.lang.String)
	 */
	@Override
	public void subTask(String name) {
		this.monitor.subTask(name);
		this.job.setName(name);
	}

	/**
	 * @see org.eclipse.core.runtime.IProgressMonitor#worked(int)
	 */
	@Override
	public void worked(int work) {
		this.monitor.worked(work);
	}
}