package com.lyncc.netty.keepalive;

import java.util.Collection;

public class Utils {

	/**
	 * 判断对象是否为空<br />
	 * 不为空返回true，为空返回false
	 * 
	 * @param o
	 * @return
	 */
	public static boolean notEmpty(Object o) {
		boolean notEmpty = false;
		if (o instanceof String) {
			String s = (String) o;
			if (s != null && !"".equals(s) && !"undefined".equals(s)
					&& !"null".equals(s)) {
				notEmpty = true;
			}
		} else if (o instanceof Collection) {
			Collection c = (Collection) o;
			if (c != null && c.size() > 0) {
				notEmpty = true;
			}
		} else if (o instanceof Object[]) {
			Object[] arr = (Object[]) o;
			if (arr != null && arr.length > 0) {
				notEmpty = true;
			}
		} else if (o != null) {
			return true;
		}
		return notEmpty;
	}
    
}
