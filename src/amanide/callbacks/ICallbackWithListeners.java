package amanide.callbacks;

public interface ICallbackWithListeners<X> {

	Object call(X obj);

	void registerListener(ICallbackListener<X> listener);

	void unregisterListener(ICallbackListener<X> listener);
}