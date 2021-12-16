package com.contrastsecurity;

public final class BinaryScope {

	private final ThreadLocal<Counter> counter = new ThreadLocal<Counter>() {
		@Override
		protected Counter initialValue() {
			return new Counter();
		}
	};

	public boolean inScope() {
		return counter.get().value != 0;
	}

	public boolean inOutermostScope() {
		return counter.get().value == 1;
	}

	public boolean inNestedScope() {
		return counter.get().value > 1;
	}

	public void enterScope() {
		counter.get().value++;
	}

	public void leaveScope() {
		counter.get().value--;
	}

	@Override
	public String toString() {
		return String.valueOf(counter.get().value);
	}

	private static final class Counter {
		private int value;
	}
}