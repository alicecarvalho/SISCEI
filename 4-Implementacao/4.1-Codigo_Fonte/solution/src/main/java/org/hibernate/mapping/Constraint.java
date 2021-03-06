/*
 * Hibernate, Relational Persistence for Idiomatic Java Copyright (c) 2010, Red
 * Hat Inc. or third-party contributors as indicated by the @author tags or
 * express copyright attribution statements applied by the authors. All
 * third-party contributions are distributed under license by Red Hat Inc. This
 * copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. You should have received a copy of the GNU Lesser General Public
 * License along with this distribution; if not, write to: Free Software
 * Foundation, Inc. 51 Franklin Street, Fifth Floor Boston, MA 02110-1301 USA
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;

/**
 * A relational constraint.
 * 
 * @author Gavin King
 * @author Brett Meyer
 */
public abstract class Constraint implements RelationalModel, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 9067630692598930493L;
	
	private String name;
	@SuppressWarnings("rawtypes")
	private final ArrayList columns = new ArrayList();
	private Table table;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * If a constraint is not explicitly named, this is called to generate a
	 * unique hash using the table and column names. Static so the name can be
	 * generated prior to creating the Constraint. They're cached, keyed by
	 * name, in multiple locations.
	 * 
	 * @param prefix
	 *            Appended to the beginning of the generated name
	 * @param table
	 * @param columns
	 * @return String The generated name
	 */
	public static String generateName(String prefix, Table table,
			Column... columns)
	{
		Column[] alphabeticalColumns = columns.clone();
		Arrays.sort(alphabeticalColumns, ColumnComparator.INSTANCE);

		String constraintName = table.getName();
		for (Column column : alphabeticalColumns)
		{
			if (column != null)
			{
				constraintName += (constraintName.isEmpty() ? column.getName()
						: "_" + column.getName());
			}
		}

		if (constraintName.length() > 60)
		{
			return prefix + constraintName.substring(0, 59);
		}

		return prefix + constraintName;
	}

	/**
	 * Helper method for {@link #generateName(String, Table, Column...)}.
	 * 
	 * @param prefix
	 *            Appended to the beginning of the generated name
	 * @param table
	 * @param columns
	 * @return String The generated name
	 */
	public static String generateName(String prefix, Table table,
			List<Column> columns)
	{
		return generateName(prefix, table,
				columns.toArray(new Column[columns.size()]));
	}

	/**
	 * Hash a constraint name using MD5. Convert the MD5 digest to base 35 (full
	 * alphanumeric), guaranteeing that the length of the name will always be
	 * smaller than the 30 character identifier restriction enforced by a few
	 * dialects.
	 * 
	 * @param s
	 *            The name to be hashed.
	 * @return String The hased name.
	 */
	public static String hashedName(String s)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.reset();
			md.update(s.getBytes());
			byte[] digest = md.digest();
			BigInteger bigInt = new BigInteger(1, digest);
			return bigInt.toString(35);
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new HibernateException(
					"Unable to generate a hashed Constraint name!", e);
		}
	}

	private static class ColumnComparator implements Comparator<Column>
	{
		public static ColumnComparator INSTANCE = new ColumnComparator();

		@Override
		public int compare(Column col1, Column col2)
		{
			return col1.getName().compareTo(col2.getName());
		}
	}

	@SuppressWarnings("rawtypes")
	public Iterator getColumnIterator()
	{
		return columns.iterator();
	}

	@SuppressWarnings("unchecked")
	public void addColumn(Column column)
	{
		if (!columns.contains(column)) columns.add(column);
	}

	@SuppressWarnings("rawtypes")
	public void addColumns( Iterator columnIterator)
	{
		while (columnIterator.hasNext())
		{
			Selectable col = (Selectable) columnIterator.next();
			if (!col.isFormula()) addColumn((Column) col);
		}
	}

	/**
	 * @param column
	 * @return true if this constraint already contains a column with same name.
	 */
	public boolean containsColumn(Column column)
	{
		return columns.contains(column);
	}

	public int getColumnSpan()
	{
		return columns.size();
	}

	public Column getColumn(int i)
	{
		return (Column) columns.get(i);
	}

	@SuppressWarnings("rawtypes")
	public Iterator columnIterator()
	{
		return columns.iterator();
	}

	public Table getTable()
	{
		return table;
	}

	public void setTable(Table table)
	{
		this.table = table;
	}

	public boolean isGenerated(Dialect dialect)
	{
		return true;
	}

	@Override
	public String sqlDropString(Dialect dialect, String defaultCatalog,
			String defaultSchema)
	{
		if (isGenerated(dialect))
		{
			return new StringBuilder()
					.append("alter table ")
					.append(getTable().getQualifiedName(dialect,
							defaultCatalog, defaultSchema))
					.append(" drop constraint ")
					.append(dialect.quote(getName())).toString();
		}
		else
		{
			return null;
		}
	}

	@Override
	public String sqlCreateString(Dialect dialect, Mapping p,
			String defaultCatalog, String defaultSchema)
	{
		if (isGenerated(dialect))
		{
			String constraintString = sqlConstraintString(dialect, getName(),
					defaultCatalog, defaultSchema);
			StringBuilder buf = new StringBuilder("alter table ").append(
					getTable().getQualifiedName(dialect, defaultCatalog,
							defaultSchema)).append(constraintString);
			return buf.toString();
		}
		else
		{
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	public List getColumns()
	{
		return columns;
	}

	public abstract String sqlConstraintString(Dialect d,
			String constraintName, String defaultCatalog, String defaultSchema);

	@Override
	public String toString()
	{
		return getClass().getName() + '(' + getTable().getName() + getColumns()
				+ ") as " + name;
	}

	/**
	 * @return String The prefix to use in generated constraint names. Examples:
	 *         "UK_", "FK_", and "PK_".
	 */
	public abstract String generatedConstraintNamePrefix();
}
