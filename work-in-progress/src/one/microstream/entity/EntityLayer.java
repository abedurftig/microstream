package one.microstream.entity;

public abstract class EntityLayer extends Entity.AbstractAccessible
{
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	private Entity inner;
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////
	
	public EntityLayer(final Entity inner)
	{
		super();
		this.inner = inner; // may be null in case of delayed initialization.
	}

	
	
	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////
	
	@Override
	protected Entity $entityIdentity()
	{
		// the data instance (and only that) has a back-reference to the actual entity instance it belongs to.
		return Entity.identity(this.inner);
	}
	
	@Override
	protected Entity $entityData()
	{
		return Entity.data(this.inner);
	}
	
	@Override
	protected boolean $updateEntityData(final Entity newData)
	{
		/*
		 *  if the inner layer instance reports success, it is an intermediate layer.
		 *  Otherwise, it is the data itself and needs to be replaced.
		 */
		if(!Entity.updateData(this.inner, newData))
		{
			this.$updateDataValidating(newData);
		}
		
		return true;
	}
	
	protected Entity $inner()
	{
		return this.inner;
	}
	
	protected void $validateNewData(final Entity newData)
	{
		// (18.07.2019 TM)FIXME: check for same data class necessary?
		if(Entity.identity(newData) != this.$entityIdentity())
		{
			// (10.12.2017 TM)EXCP: proper exception
			throw new RuntimeException("Entity identity mismatch.");
		}
	}
	
	protected void $updateDataValidating(final Entity newData)
	{
		final Entity actualNewData = Entity.data(newData);
		this.$validateNewData(actualNewData);
		this.$setInner(actualNewData);
	}
	
	protected void $setInner(final Entity inner)
	{
		this.inner = inner;
	}
	
}
