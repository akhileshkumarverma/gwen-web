/*
 * Copyright 2014-2015 Branko Juric, Brady Wood
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gwen.web

import java.util.ArrayList
import org.openqa.selenium.By
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.WebElement
import gwen.Predefs.Kestrel
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
  * Locates web elements using the selenium web driver.
  * 
  * @author Branko Juric, Brady Wood
  */
trait WebElementLocator extends LazyLogging {
  
  /**
    * Locates a bound web element.
    * 
    *  @param env the web environment context
    *  @param elementBinding the web element locator binding
    *  @return the found element (errors if not found)
    */
  def locate(env: WebEnvContext, elementBinding: LocatorBinding): WebElement = 
    locateElement(env, elementBinding) match {
      case Some(webElement) => webElement
      case None => throw new NoSuchElementException(s"Web element not found: ${elementBinding.element}")
    }
  
  /**
   * Locates a bound web element.
   * 
   * @param env the web environment context
   * @param elementBinding the web element locator binding
   * @return Some(element) if found, None otherwise
   */
  def locateOpt(env: WebEnvContext, elementBinding: LocatorBinding): Option[WebElement] = try {
    locateElement(env, elementBinding)
  } catch {
    case e: TimeoutOnWaitException => None
  }
  
  /**
   * Locates a bound web element.
   * 
   * @param env the web environment context
   * @param elementBinding the web element locator binding
   * @return Some(element) if found, None otherwise
   */
  private def locateElement(env: WebEnvContext, elementBinding: LocatorBinding): Option[WebElement] = {
     logger.debug(s"Locating ${elementBinding.element}")
     try {
       findElementByLocator(env, elementBinding)
     } catch {
        case e: WebDriverException =>
        // attempt to locate one more time on web driver exception
        findElementByLocator(env, elementBinding)
     }
  }
  
  /** Finds an element by the given locator expression. */
  private def findElementByLocator(env: WebEnvContext, elementBinding: LocatorBinding): Option[WebElement] = {
    val lookup = elementBinding.lookup
    val locator = elementBinding.locator 
    (locator match {
      case "id" => getElement(env, By.id(lookup))
      case "name" => getElement(env, By.name(lookup))
      case "tag name" => getElement(env, By.tagName(lookup))
      case "css selector" => getElement(env, By.cssSelector(lookup))
      case "xpath" => getElement(env, By.xpath(lookup))
      case "class name" => getElement(env, By.className(lookup))
      case "link text" => getElement(env, By.linkText(lookup))
      case "partial link text" => getElement(env, By.partialLinkText(lookup))
      case "javascript" => getElementByJavaScript(env, s"$lookup")
      case _ => throw new LocatorBindingException(elementBinding.element, s"unsupported locator: ${locator}")
    }) tap { optWebElement =>
      optWebElement foreach { webElement =>
        if (!webElement.isDisplayed()) {
          env.scrollIntoView(webElement, ScrollTo.top)
        }
        env.highlight(webElement)
      }
    }
  }
  
  /**
    * Gets a web element using the given by locator.
    * 
    * @param env the web environment context
    * @param by the by locator
    */
  private def getElement(env: WebEnvContext, by: By): Option[WebElement] = 
    Option(env.withWebDriver(_.findElement(by)))
    
  /**
    * Gets a web element by the given javascript expression. If the web element is not 
    * visible in the browser, then the element is brought into view by scrolling to it.
    * 
    * @param env the web environment context
    * @param javascipt the javascript expression for returning the element
    */
  private def getElementByJavaScript(env: WebEnvContext, javascript: String): Option[WebElement] = {
    var elem: Option[WebElement] = None
    env.waitUntil {
      elem = env.executeScript(s"return $javascript") match {
        case elems: ArrayList[_] => 
          if (!elems.isEmpty()) Option(elems.get(0).asInstanceOf[WebElement])
          else None
        case elem => Option(elem) match {
          case Some(elem) => Option(elem.asInstanceOf[WebElement])
          case None => None
        }
      }
      elem.isDefined
    }
    elem
  }
  
}

/** 
  *  Captures a web element locator binding. 
  *  
  *  @param element the web element name
  *  @param locator the locator type
  *  @param lookup the lookup string
  */
case class LocatorBinding(val element: String, val locator: String, val lookup: String)

/** Thrown when a web element cannot be located. */
class LocatorBindingException(element: String, causeMsg: String) extends Exception(s"Could not locate ${element}: ${causeMsg}")