import XCTest

final class CardCreationUITests: XCTestCase {

    private var app: XCUIApplication!

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    func testCreateCardFlow_addsCardToHomeList() {
        step(#"[1] Нажать "Создать карточку""#) {
            app.buttons["home_create_card"].tap()
        }

        step(#"[2] Ввести имя и фамилию"#) {
            typeText(in: element(id: "create_first_name"), text: "TestName")
            typeText(in: element(id: "create_last_name"), text: "TestSurname")
        }

        step(#"[3] Нажать "Создать""#) {
            app.buttons["create_submit"].tap()
        }

        step(#"[4] Проверить, что карточка появилась"#) {
            XCTAssertTrue(app.staticTexts["TestName TestSurname"].waitForExistence(timeout: 2.0))
        }
    }
}

private func element(id: String) -> XCUIElement {
    let app = XCUIApplication()
    let candidates: [XCUIElement] = [
        app.textFields[id],
        app.secureTextFields[id],
        app.otherElements[id],
    ]
    for el in candidates where el.exists {
        return el
    }
    // Fallback: search anywhere by accessibilityIdentifier
    return app.descendants(matching: .any).matching(identifier: id).firstMatch
}

private func typeText(in element: XCUIElement, text: String) {
    XCTAssertTrue(element.waitForExistence(timeout: 2.0), "Element not found: \(element)")
    element.tap()
    element.typeText(text)
}

private func step(_ title: String, _ body: () -> Void) {
    XCTContext.runActivity(named: title) { _ in
        body()
    }
}

