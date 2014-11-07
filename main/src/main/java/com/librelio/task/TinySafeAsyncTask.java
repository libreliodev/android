package com.librelio.task;

import java.util.concurrent.RejectedExecutionException;


public abstract class TinySafeAsyncTask<Params, Progress, Result> extends TinyAsyncTask<Params, Progress, Result> {
	public void safeExecute(Params... params) {
		try {
			execute(params);
		} catch(RejectedExecutionException e) {
			// Failed to start in the background, so do it in the foreground
			onPreExecute();
			if (isCancelled()) {
				onCancelled();
			} else {
				onPostExecute(doInBackground(params));
			}
		}
	}
}
