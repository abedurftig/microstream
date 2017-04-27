package net.jadoth.swizzling.types;

public interface SwizzleTypeIdentity extends SwizzleTypeIdOwner
{
	@Override
	public long typeId();

	public String typeName();

	
	
	public static int hashCode(final SwizzleTypeIdentity typeIdentity)
	{
		return Long.hashCode(typeIdentity.typeId()) & typeIdentity.typeName().hashCode();
	}

	public static boolean equals(
		final SwizzleTypeIdentity ti1,
		final SwizzleTypeIdentity ti2
	)
	{
		return ti1 == ti2
			|| ti1 != null && ti2 != null
			&& ti1.typeId() == ti2.typeId()
			&& ti1.typeName().equals(ti2.typeName())
		;
	}

}
