package com.davebsoft.sctw

import _root_.scala.swing.event.{ButtonClicked, SelectionChanged, WindowClosing}
import filter.{FilterSet, TagUsers}
import java.awt.event.{ActionEvent, ActionListener, KeyEvent}
import java.awt.{Dimension}
import scala.swing._
import scala.xml._
import TabbedPane._
import twitter.{FriendsDataProvider, FollowersDataProvider, TweetsProvider}
import ui.{StatusTableModel, FiltersPane, StatusPane, FriendsFollowersPane, LoginDialog}
import state.StateRepository

/**
 * “Simple Twitter Client”
 *
 * Your feedback is welcome!
 *
 * @Author Dave Briccetti, daveb@davebsoft.com, @dcbriccetti
 */
object Main extends GUIApplication {
  private var username: String = ""
  private var password: String = ""

  /**
   * Creates the Swing frame.
   */
  def top = {

    val filterSet = new FilterSet
    val tweetsProvider = new TweetsProvider(username, password, StateRepository.get("highestId", null))
    val followers = new FollowersDataProvider(username, password).getUsers
    val statusTableModel = new StatusTableModel(tweetsProvider, getIds(followers), filterSet, username)
    val filtersPane = new FiltersPane(statusTableModel, filterSet)
    val statusPane = new StatusPane(statusTableModel, filtersPane)

    val clearAction = statusPane.clearAction
    new Frame {
      title = "Simple Twitter Client"

      TagUsers.load

      val filtersPage = new Page("Filters", filtersPane)

      val tabbedPane = new TabbedPane() {
        preferredSize = new Dimension(900, 600)

        pages.append(new Page("Tweets", statusPane))

        val following = new FriendsDataProvider(username, password).getUsers
        pages.append(new Page("Following", new FriendsFollowersPane(following, getIds(followers))))
        pages.append(new Page("Followers", new FriendsFollowersPane(followers, getIds(following))))

        pages.append(filtersPage)
      }
      listenTo(tabbedPane.selection)
      contents = tabbedPane
      var lastSelectedPane = tabbedPane.selection.page

      reactions += {
        case WindowClosing(_) => {
          StateRepository.set("highestId", tweetsProvider.getHighestId)
          StateRepository.save
          TagUsers.save
          System.exit(1)
        }
        case SelectionChanged(sc) => {
          val selectedPage = tabbedPane.selection.page
          if (lastSelectedPane == filtersPage && selectedPage != filtersPage) {
            filtersPane.applyChanges
          }
          lastSelectedPane = selectedPage
        }
      }

      peer.setLocationRelativeTo(null)
    }
  }

  private def getIds(users: List[Node]): List[String] = {
    users map (u => (u \ "id").text)
  }

  def main(args: Array[String]): Unit = {

    def startUp(userName: String, pwd: String) {
      username = userName
      password = pwd
      setUpUi
    }

    def shutDown = System.exit(1)

    new LoginDialog(new twitter.AuthenticationProvider, shutDown, startUp).display
  }

  def setUpUi {
    init
    top.pack
    top.visible = true
  }

}