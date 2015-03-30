package amanide.callbacks;

public interface ICallback<Ret, Arg> {

	Ret call(Arg arg);
}
