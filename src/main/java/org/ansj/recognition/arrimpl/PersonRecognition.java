package org.ansj.recognition.arrimpl;

import org.ansj.domain.AnsjItem;
import org.ansj.domain.PersonNatureAttr;
import org.ansj.domain.Term;
import org.ansj.domain.TermNatures;
import org.ansj.library.DATDictionary;
import org.ansj.recognition.TermArrRecognition;
import org.ansj.util.Graph;
import org.ansj.util.TermUtil;
import org.nlpcn.commons.lang.util.logging.Log;
import org.nlpcn.commons.lang.util.logging.LogFactory;
import org.nlpcn.commons.lang.viterbi.Node;
import org.nlpcn.commons.lang.viterbi.Viterbi;
import org.nlpcn.commons.lang.viterbi.function.Score;
import org.nlpcn.commons.lang.viterbi.function.Values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 人名识别工具类
 *
 * @author ansj
 */
public class PersonRecognition implements TermArrRecognition {

	private static final int B = 0, C = 1, D = 2, E = 3, K = 4, L = 5, M = 6, X = 7, Y = 8, Z = 9, A = 10;

	private static final Log LOG = LogFactory.getLog();

	//BE BCD  XD BZ
	private static final Map<Integer, Double> transition = new HashMap<>();

	//0B 1C 2D 3E 4K 5L 6M 7X 8Y 9Z 10A
	static {
		transition.put(X * 1000 + D, 0.35999);
		transition.put(Y * 1000 + B, -3.73687);
		transition.put(Y * 1000 + M, -0.43878);
		transition.put(Y * 1000 + L, 0.28621);
		transition.put(Z * 1000 + X, -2.52373);
		transition.put(Z * 1000 + Y, -3.11504);
		transition.put(Z * 1000 + B, -1.83448);
		transition.put(Z * 1000 + L, 0.26402);
		transition.put(Z * 1000 + M, -0.06501);
		transition.put(Y * 1000 + Y, -1.88320);
		transition.put(Y * 1000 + X, -4.32692);
		transition.put(K * 1000 + Y, 0.28621);
		transition.put(K * 1000 + X, -0.49013);
		transition.put(E * 1000 + M, -0.49013);
		transition.put(K * 1000 + B, 0.42897);
		transition.put(D * 1000 + L, 0.48905);
		transition.put(D * 1000 + M, -0.10071);
		transition.put(L * 1000 + A, -0.02154);
		transition.put(C * 1000 + D, 0.76553);
		transition.put(E * 1000 + Y, -3.73687);
		transition.put(E * 1000 + X, -4.02912);
		transition.put(M * 1000 + B, -0.30355);
		transition.put(D * 1000 + B, -2.05756);
		transition.put(L * 1000 + K, -0.49884);
		transition.put(M * 1000 + Y, -0.43878);
		transition.put(M * 1000 + X, -0.78341);
		transition.put(E * 1000 + B, -2.80074);
		transition.put(D * 1000 + X, -2.94393);
		transition.put(D * 1000 + Y, -3.83297);
		transition.put(A * 1000 + K, -0.15840);
		transition.put(A * 1000 + A, 0.89299);
		transition.put(E * 1000 + L, 0.46686);
		transition.put(B * 1000 + E, 0.79864);
		transition.put(B * 1000 + C, 0.76553);
		transition.put(B * 1000 + Z, 0.26402);

		//0B 1C 2D 3E 4K 5L 6M 7X 8Y 9Z 10A

	}

	private PersonNode[][] nodes = null;

	private int beginOff;

	@Override
	public void recognition(Graph graph) {

		Term[] terms = graph.terms;

		Term from = null, first = null, sencond = null, third = null, last = null;

		PersonNatureAttr fromPna = null, fPna = null, sPna = null, tPna = null, lPna = null;


		//0B 1C 2D 3E 4K 5L 6M 7X 8Y 9Z 10A

		nodes = new PersonNode[terms.length + 1][11];

		beginOff = terms[0].getOffe();


		for (int i = 0; i < terms.length - 1; i++) {
			first = terms[i];

			if (first == null) {
				continue;
			}

			fPna = getPersonNature(first);
			setNode(first, A);

			if (fPna.getU() > 0) { //人名的上文和姓成词
				String name = first.getName();
				if (name.length() == 2) {
					setAllNode(first.from().getName() + name.charAt(0), first.getOffe(), true);
					setAllNode(String.valueOf(name.charAt(0)), first.getOffe(), false);
					setAllNode(String.valueOf(name.charAt(1)), first.getOffe() + 1, false);
				} else if (name.length() == 3) {
					setAllNode(first.from().getName() + name.charAt(0), first.getOffe(), true);
					setAllNode(String.valueOf(name.charAt(0)), first.getOffe(), false);
					setAllNode(String.valueOf(name.charAt(1)), first.getOffe() + 1, false);
					setAllNode(String.valueOf(name.charAt(2)), first.getOffe() + 2, false);
					setAllNode(name.substring(1), first.getOffe(), true);
				} else {
					LOG.warn("length out of person recognition so skip U : " + name);
				}
			}


			if (fPna.getV() > 0) { //人名的上文和姓成词
				String name = first.getName();
				if (name.length() == 2) {
					setAllNode(String.valueOf(name.charAt(0)), first.getOffe(), false);
					setAllNode(String.valueOf(name.charAt(1)), first.getOffe() + 1, false);
					setAllNode(name.charAt(1) + first.to().getName(), first.getOffe() + 1, true);
				} else if (name.length() == 3) {
					setAllNode(String.valueOf(name.charAt(0)), first.getOffe(), false);
					setAllNode(String.valueOf(name.charAt(1)), first.getOffe() + 1, false);
					setAllNode(String.valueOf(name.charAt(2)), first.getOffe() + 2, false);
					setAllNode(name.charAt(2) + first.to().getName(), first.getOffe() + 2, true);
					setAllNode(name.substring(0, 2), first.getOffe(), true);
				} else {
					LOG.warn("length out of person recognition so skip V : " + name);
				}
			}

			if (fPna == null || !fPna.isActive()) {
				continue;
			}

			sencond = first.to();
			if (sencond.getOffe() == terms.length || sencond.getName().length() > 2) { //说明到结尾了,或者后面长度不符合规则
				continue;
			}

			third = sencond.to();
			from = first.from();

			//XD
			if (first.getName().length() == 2) {
				setNode(from, K);
				setNode(from, M);
				setNode(first, X);
				setNode(sencond, D);
				setNode(third, M);
				setNode(third, L);
				continue;
			}

			setNode(from, K);
			setNode(from, M);
			setNode(first, B);
			setNode(third, M);
			setNode(third, L);
			//BZ
			if (sencond.getName().length() == 2) {
				setNode(sencond, Z);
				continue;
			} else {//BE
				setNode(sencond, E);
			}


			if (third.getOffe() == terms.length || third.getName().length() > 1) { //说明到结尾了,或者后面长度不符合规则
				continue;
			}

			//BCD
			setNode(first, B);
			setNode(sencond, C);
			setNode(third, D);
			setNode(third.to(), M);
			setNode(third.to(), L);

		}
		PersonNatureAttr begin = DATDictionary.person("BEGIN");

		nodes[0][6] = null;
		nodes[0][4] = new PersonNode(4, "B", -Math.log(begin.getK()));
		nodes[0][10] = new PersonNode(10, "B", -Math.log(begin.getA()));

		PersonNatureAttr end = DATDictionary.person("END");
		nodes[terms.length][5] = new PersonNode(5, "E", -Math.log(end.getL()));
		nodes[terms.length][6] = null;
		nodes[terms.length][10] = new PersonNode(10, "E", -Math.log(end.getA()));

		Viterbi<PersonNode> viterbi = new Viterbi<PersonNode>(nodes, new Values<PersonNode>() {
			@Override
			public int step(Node<PersonNode> node) {
				return node.getObj().name.length();
			}

			@Override
			public double selfSscore(Node<PersonNode> node) {
				return node.getObj().score;
			}

		});


		List<PersonNode> result = viterbi.compute(new Score<PersonNode>() {
			@Override
			public Double score(Node<PersonNode> from, Node<PersonNode> to) {
				if (from == null || to == null) {
					return null;
				}
				Double tValue = transition.get(from.getT().tag * 1000 + to.getT().tag);
				if (tValue == null || from.getScore() == null) {
					return null;
				}
				return from.getScore() + to.getSelfScore() + tValue;
			}

			@Override
			public boolean sort() {
				return true;
			}
		});


		//BE BCD  XD BZ
		//int B = 0, C = 1, D = 2, E = 3, K = 4, L = 5, M = 6, X = 7, Y = 8, Z = 9, A = 10;

		int off = 0;

		int len = result.size() - 1;
		for (int i = 1; i < len; i++) {
			PersonNode p1 = result.get(i), p2 = null;

			if (p1.tag != B && p1.tag != X) {
				off += p1.name.length();
				continue;
			}

			List<Term> tempList = new ArrayList<>();
			tempList.add(terms[off]);
			off += p1.name.length();


			for (int j = i + 1; j < result.size(); j++) {
				p2 = result.get(j);
				tempList.add(terms[off]);
				off += p2.name.length();
				if (p2.tag == E || p2.tag == D || p2.tag == Z) {
					TermUtil.insertTerm(terms, tempList, TermNatures.NR);
					i = j;
					break;
				}
			}
		}

	}

	/**
	 * 把所有可能放入到图中
	 *
	 * @param name
	 * @param offe
	 * @param skip 是否跳过不再词典中的词语
	 */
	private void setAllNode(String name, int offe, boolean skip) {
		AnsjItem item = DATDictionary.getItem(String.valueOf(name));
		if (skip && item == AnsjItem.NULL || item.getStatus() < 2) {
			return;
		}
		Term term = new Term(name, offe, item);
		for (int j = 0; j < 11; j++) {
			setNode(term, j);
		}
	}

	private void setNode(Term term, int tag) {
		int index = term.getOffe() - beginOff + 1;
		PersonNatureAttr pna = getPersonNature(term);
		double score = 0D;

		//0B 1C 2D 3E 4K 5L 6M 7X 8Y 9Z 10A
		switch (tag) {
			case B:
				score = pna.getB();
				break;
			case C:
				score = pna.getC();
				break;
			case D:
				score = pna.getD();
				break;
			case E:
				score = pna.getE();
				break;
			case K:
				score = pna.getK();
				break;
			case L:
				score = pna.getL();
				break;
			case M:
				score = pna.getM();
				break;
			case X:
				score = pna.getX();
				break;
			case Y:
				score = pna.getY();
				break;
			case Z:
				score = pna.getZ();
				break;
			case A:
				score = pna.getA();
				break;
		}

		if (nodes[index][tag] == null) {
			nodes[index][tag] = new PersonNode(tag, term.getName(), score);
		}
	}


	/**
	 * 获得一个term的personnature
	 *
	 * @param term
	 * @return
	 */
	private PersonNatureAttr getPersonNature(Term term) {

		if (term.termNatures().personAttr != PersonNatureAttr.NULL) {
			return term.termNatures().personAttr;
		}

		PersonNatureAttr person = DATDictionary.person(term.getName()); //解决词典外单字问题

		if (person != null) {
			return person;
		}

		person = DATDictionary.person(":" + term.getNatureStr());

		if (person == null) {
			return PersonNatureAttr.NULL;
		}

		return person;
	}

	class PersonNode {
		public int tag;
		public String name;
		public double score;

		public PersonNode(int tag, String name, double score) {
			this.tag = tag;
			this.name = name;
			this.score = score;
		}

		public String toString() {
			return name + "," + tag + "," + score;
		}
	}

}
