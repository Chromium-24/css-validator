// $Id$
// Author: Yves Lafon <ylafon@w3.org>
//
// (c) COPYRIGHT MIT, ERCIM and Keio University, 2013.
// Please first read the full copyright statement in file COPYRIGHT.html
package org.w3c.css.properties.css3;

import org.w3c.css.util.ApplContext;
import org.w3c.css.util.InvalidParamException;
import org.w3c.css.values.CssExpression;
import org.w3c.css.values.CssIdent;
import org.w3c.css.values.CssLayerList;
import org.w3c.css.values.CssNumber;
import org.w3c.css.values.CssTypes;
import org.w3c.css.values.CssValue;
import org.w3c.css.values.CssValueList;

import java.util.ArrayList;

import static org.w3c.css.values.CssOperator.COMMA;
import static org.w3c.css.values.CssOperator.SPACE;

/**
 * @spec http://www.w3.org/TR/2012/CR-css3-speech-20120320/#voice-props-voice-family
 */
public class CssVoiceFamily extends org.w3c.css.properties.css.CssVoiceFamily {

	public static final ArrayList<CssIdent> genericVoices, age;
	public static final CssIdent preserve;

	static {
		preserve = CssIdent.getIdent("preserve");

		String[] _genericVoices = {"male", "female", "neutral"};
		genericVoices = new ArrayList<CssIdent>(_genericVoices.length);
		for (String s : _genericVoices) {
			genericVoices.add(CssIdent.getIdent(s));
		}

		String[] _age = {"child", "young", "old"};
		age = new ArrayList<CssIdent>(_age.length);
		for (String s : _age) {
			age.add(CssIdent.getIdent(s));
		}

	}

	static CssIdent getGenericVoiceName(CssIdent ident) {
		for (CssIdent id : genericVoices) {
			if (id.equals(ident)) {
				return id;
			}
		}
		return null;
	}

	static CssIdent getAge(CssIdent ident) {
		for (CssIdent id : age) {
			if (id.equals(ident)) {
				return id;
			}
		}
		return null;
	}

	/**
	 * Create a new CssVoiceFamily
	 */
	public CssVoiceFamily() {
		value = initial;
	}

	/**
	 * Creates a new CssVoiceFamily
	 *
	 * @param expression The expression for this property
	 * @throws org.w3c.css.util.InvalidParamException
	 *          Expressions are incorrect
	 */
	public CssVoiceFamily(ApplContext ac, CssExpression expression, boolean check)
			throws InvalidParamException {
		ArrayList<CssValue> values = new ArrayList<CssValue>();

		while (!expression.end()) {
			char op = expression.getOperator();
			CssValue val = expression.getValue();
			switch (val.getType()) {
				case CssTypes.CSS_STRING:
				case CssTypes.CSS_IDENT:
					ArrayList<CssValue> cssValues = new ArrayList<CssValue>();
					cssValues.add(val);
					// we add idents if separated by spaces...
					while (op == SPACE && expression.getRemainingCount() > 1) {
						expression.next();
						op = expression.getOperator();
						val = expression.getValue();
						int type = val.getType();
						if (type == CssTypes.CSS_IDENT ||
								type == CssTypes.CSS_STRING ||
								type == CssTypes.CSS_NUMBER) {
							cssValues.add(val);
						} else {
							throw new InvalidParamException("value", val,
									getPropertyName(), ac);
						}
					}
					checkExpression(ac, values, cssValues, check);
					break;
				default:
					throw new InvalidParamException("value", val,
							getPropertyName(), ac);
			}
			expression.next();
			if (!expression.end() && (op != COMMA)) {
				throw new InvalidParamException("operator",
						((new Character(op)).toString()), ac);
			}
		}
		value = (values.size() > 1) ? new CssLayerList(values) : values.get(0);
		checkValues(ac, values);
	}

	public CssVoiceFamily(ApplContext ac, CssExpression expression)
			throws InvalidParamException {
		this(ac, expression, false);
	}

	private void checkExpression(ApplContext ac, ArrayList<CssValue> curval,
								 ArrayList<CssValue> values, boolean check)
			throws InvalidParamException {
		CssIdent ageVal = null;
		CssValue numVal = null;
		CssValue nameVal = null;
		ArrayList<CssIdent> identl = null;

		// so we can have a generic voice or a string or an ident list...
		// let's triage things... that will require extra checks (again)
		// at the end *sigh*
		for (CssValue val : values) {
			if (ageVal == null && nameVal == null && val.getType() == CssTypes.CSS_IDENT) {
				ageVal = getAge((CssIdent) val);
				if (ageVal != null) {
					continue;
				}
			}
			if (numVal == null) {
				switch (val.getType()) {
					case CssTypes.CSS_STRING:
						if (nameVal != null) {
							// don't mix string and everything else.
							throw new InvalidParamException("value", val,
									getPropertyName(), ac);
						}
						nameVal = val;
						break;
					case CssTypes.CSS_IDENT:
						CssIdent id = (CssIdent) val;
						if (inherit.equals(id)) {
							val = inherit;
						} else if (preserve.equals(id)) {
							val = preserve;
						}
						if (nameVal == null) {
							nameVal = val;
							identl = new ArrayList<CssIdent>();
						}
						if (identl == null) {
							// else we got a string before...
							throw new InvalidParamException("value", val,
									getPropertyName(), ac);
						}
						identl.add((CssIdent) val);
						break;
					case CssTypes.CSS_NUMBER:
						CssNumber n = val.getNumber();
						n.checkStrictPositiveness(ac, this);
						numVal = val;
						break;
				}
			} else {
				// we can't have things after the number.
				throw new InvalidParamException("value", val,
						getPropertyName(), ac);
			}
		}
		if (values.size() == 1) {
			// that's the easy one...
			if (nameVal == null) {
				throw new InvalidParamException("value", values.get(0),
						getPropertyName(), ac);
			}
			if (nameVal.getType() == CssTypes.CSS_IDENT) {
				hasGenericVoiceFamily = (getGenericVoiceName((CssIdent) nameVal) != null);
			}
			curval.add(nameVal);

		} else {
			// let the fun begin...
			if (numVal != null || ageVal != null) {
				// we must have a generic name...
				// this also checks that we don't have a String.
				if (identl == null || identl.size() != 1) {
					CssValue v = new CssValueList(values);
					throw new InvalidParamException("value", v,
							getPropertyName(), ac);
				}
				// ok so now check that we have a generic value in nameVal
				if (getGenericVoiceName((CssIdent) nameVal) == null) {
					throw new InvalidParamException("value", nameVal,
							getPropertyName(), ac);
				}
			} else {
				if (identl != null) {
					if (identl.size() > 1) {
						ac.getFrame().addWarning("with-space", 1);
					} else {
						hasGenericVoiceFamily = (getGenericVoiceName(identl.get(0)) != null);
					}
				}
			}
			// now addthe value...
			curval.add(new CssValueList(values));

		}
	}

	// final consistency check

	private void checkValues(ApplContext ac, ArrayList<CssValue> values)
			throws InvalidParamException {
		// we need to check that we don't have 'inherit' in multiple values
		if (values.size() > 1) {
			for (CssValue val : values) {
				if ((inherit == val) || (preserve == val)) {
					throw new InvalidParamException("unrecognize", ac);
				}
			}
		}
		if (inherit != value && preserve != value && !hasGenericVoiceFamily) {
			ac.getFrame().addWarning("no-generic-family", getPropertyName());
		}
	}
}

