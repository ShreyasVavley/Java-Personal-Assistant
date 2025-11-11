import javax.swing.*;
import javax.swing.border.EmptyBorder; // New import
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/**
 * --- 1. The Core Bot Logic (Upgraded for Arguments) ---
 *
 * (ALL COMMAND CLASSES ARE 100% UNCHANGED)
 */

interface Command {
    String execute(String argument);
}

class TimeCommand implements Command {
    @Override
    public String execute(String argument) {
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        return "The current time is " + now.format(formatter) + ".";
    }
}

class WeatherCommand implements Command {
    private static final String API_KEY = "c98363ae6e874c7e897140545251111"; // WeatherAPI Key

    @Override
    public String execute(String argument) {
        // ... (WeatherCommand code is unchanged)
        if (API_KEY.equals("YOUR_API_KEY_HERE")) {
            return "Weather API key not set. Please get a free key from WeatherAPI.com and add it to the WeatherCommand class.";
        }
        if (argument == null || argument.trim().isEmpty()) {
            return "Please provide a city. Example: 'weather London' or 'weather 90210'";
        }
        try {
            String city = java.net.URLEncoder.encode(argument.trim(), "UTF-8");
            URL url = new URL("https://api.weatherapi.com/v1/current.json?key=" + API_KEY + "&q=" + city);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return parseWeatherJson(response.toString(), argument);
            } else if (responseCode == 400) {
                return "Sorry, I couldn't find weather for '" + argument + "'.";
            } else {
                return "Sorry, there was an error connecting to the weather service (Code: " + responseCode + ").";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Sorry, an error occurred while fetching the weather.";
        }
    }

    private String parseWeatherJson(String jsonResponse, String city) {
        // ... (parseWeatherJson code is unchanged)
        try {
            String tempKey = "\"temp_c\":";
            int tempIndex = jsonResponse.indexOf(tempKey) + tempKey.length();
            int tempEndIndex = jsonResponse.indexOf(',', tempIndex);
            String temp = jsonResponse.substring(tempIndex, tempEndIndex).trim();
            String conditionKey = "\"text\":\"";
            int conditionIndex = jsonResponse.indexOf(conditionKey) + conditionKey.length();
            int conditionEndIndex = jsonResponse.indexOf('\"', conditionIndex);
            String condition = jsonResponse.substring(conditionIndex, conditionEndIndex);
            return "The weather in " + city + " is " + condition + " with a temperature of " + temp + "°C.";
        } catch (Exception e) {
            return "Sorry, I received weather data but couldn't parse it.";
        }
    }
}

class GreetCommand implements Command {
    @Override
    public String execute(String argument) {
        return "Hello there! How can I help you today?";
    }
}

class AskCommand implements Command {

    // *** PASTE YOUR GEMINI API KEY HERE ***
    // Get one from https://aistudio.google.com/
    private static final String API_KEY = "AIzaSyBnBI5TNblqY73yNBVD8G16OoSj_xyiy_k";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-09-2025:generateContent?key=" + API_KEY;

    @Override
    public String execute(String argument) {
        if (API_KEY.equals("YOUR_GEMINI_API_KEY")) {
            return "Gemini API key not set. Please get a free key from Google AI Studio and add it to the AskCommand class.";
        }

        if (argument == null || argument.trim().isEmpty()) {
            return "Please ask a question. Example: 'ask What is the capital of France?'";
        }

        try {
            // 1. Create the JSON payload
            // We must escape quotes and newlines in the user's input
            String escapedArgument = argument.replace("\"", "\\\"").replace("\n", "\\n");
            String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedArgument + "\"}]}]}";

            // 2. Create the URL and Connection
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true); // We are sending data

            // 3. Send the request (write the JSON payload)
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 4. Read the response
            int responseCode = connection.getResponseCode();
            StringBuilder response = new StringBuilder();
            BufferedReader in;

            if (responseCode == 200) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                // If there's an error, read the error stream
                in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // 5. Parse the response
            if (responseCode == 200) {
                return parseGeminiJson(response.toString());
            } else {
                return "Error from Gemini API: " + response.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "An internal error occurred while contacting the Gemini API.";
        }
    }

    /**
     * A very simple, non-robust JSON parser to get the text we want.
     */
    private String parseGeminiJson(String jsonResponse) {
        try {
            String textKey = "\"text\": \"";
            int startIndex = jsonResponse.indexOf(textKey) + textKey.length();
            int endIndex = jsonResponse.indexOf("\"", startIndex);
            String rawText = jsonResponse.substring(startIndex, endIndex);
            
            // Unescape characters like \n
            return rawText.replace("\\n", "\n").replace("\\\"", "\"");
        } catch (Exception e) {
            // This can happen if the safety settings block the response
            if (jsonResponse.contains("promptFeedback")) {
                 return "My apologies, I can't answer that. The query was blocked for safety reasons.";
            }
            return "Sorry, I received a response from Gemini but couldn't parse it.";
        }
    }
}


class HelpCommand implements Command {
    private HashMap<String, Command> commands;

    public HelpCommand(HashMap<String, Command> commands) {
        this.commands = commands;
    }

    @Override
    public String execute(String argument) {
        StringBuilder sb = new StringBuilder("I can understand the following commands:\n");
        sb.append("- hello\n");
        sb.append("- time\n");
        sb.append("- weather [city]\n");
        sb.append("- ask [question]\n"); // <-- NEWLY ADDED
        sb.append("- help\n");
        sb.append("- exit\n");
        return sb.toString();
    }
}

/**
 * --- 2. The Bot's "Brain" ---
 *
 * (COMMANDPROCESSOR CLASS IS 100% UNCHANGED)
 */
class CommandProcessor {
    private HashMap<String, Command> commands;

    public CommandProcessor() {
        commands = new HashMap<>();
        commands.put("time", new TimeCommand());
        commands.put("weather", new WeatherCommand());
        commands.put("hello", new GreetCommand());
        commands.put("ask", new AskCommand()); // <-- NEWLY ADDED
        commands.put("help", new HelpCommand(commands));
    }

    public String processInput(String userInput) {
        // ... (processInput code is unchanged)
        String input = userInput.toLowerCase().trim();
        if (input.equals("exit")) {
            return "Goodbye!";
        }
        String[] parts = input.split(" ", 2);
        String commandName = parts[0];
        String argument = (parts.length > 1) ? parts[1] : null;
        Command command = commands.get(commandName);
        if (command != null) {
            return command.execute(argument);
        } else {
            return "Sorry, I don't understand that. Type 'help' to see what I can do.";
        }
    }
}

/**
 * --- 3. The GUI "Face" (Using Java Swing) ---
 *
 * THIS IS THE ONLY CLASS WE'VE REWRITTEN.
 * It's now styled to look like a modern chat app.
 */
public class GuiAssistant extends JFrame {

    // --- New Color Palette ---
    private static final Color COLOR_BG_DARK = new Color(0x2B2B2B);
    private static final Color COLOR_BG_INPUT = new Color(0x3C3C3C);
    private static final Color COLOR_TEXT = new Color(0xE0E0E0);
    private static final Color COLOR_BOT = new Color(0x4A90E2);
    private static final Color COLOR_USER = new Color(0x7ED321);
    private static final Color COLOR_BUTTON = new Color(0x4A90E2);

    private CommandProcessor botLogic;
    private JEditorPane chatArea; // Changed from JTextArea
    private JTextField inputField;
    private JButton sendButton;
    private StringBuilder htmlContent; // To build our chat history

    public GuiAssistant() {
        botLogic = new CommandProcessor();

        // 1. Set up the window (the JFrame)
        setTitle("Personal Assistant");
        setSize(450, 600); // Made it a bit taller
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_BG_DARK); // Set main background

        // 2. Create the HTML for the chat
        htmlContent = new StringBuilder();
        htmlContent.append("<html><head><style>");
        htmlContent.append("body { font-family: 'Inter', sans-serif; background-color: #2B2B2B; color: #E0E0E0; padding: 10px; }");
        htmlContent.append(".message { margin-bottom: 8px; padding: 10px; border-radius: 10px; max-width: 80%; }");
        htmlContent.append(".bot { background-color: #4A90E2; color: white; align-self: flex-start; }");
        htmlContent.append(".user { background-color: #7ED321; color: black; align-self: flex-end; float: right; clear: both; }");
        htmlContent.append(".speaker { font-weight: bold; margin-bottom: 4px; }");
        htmlContent.append("</style></head><body>");

        // 3. Create the Chat Area
        chatArea = new JEditorPane();
        chatArea.setEditable(false);
        chatArea.setContentType("text/html"); // Set to render HTML
        chatArea.setBackground(COLOR_BG_DARK);
        chatArea.setText(htmlContent.toString() + "</body></html>");
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(null); // Remove border from scroll pane
        add(scrollPane, BorderLayout.CENTER);

        // 4. Create the Input Panel (at the bottom)
        JPanel inputPanel = new JPanel(new BorderLayout(10, 10)); // Add spacing
        inputPanel.setBackground(COLOR_BG_INPUT);
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Add padding

        inputField = new JTextField();
        inputField.setFont(new Font("Inter", Font.PLAIN, 14));
        inputField.setBackground(COLOR_BG_INPUT);
        inputField.setForeground(COLOR_TEXT);
        // Clean underline border
        inputField.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0x555555)));

        sendButton = new JButton("➤"); // Unicode for "send" arrow
        sendButton.setFont(new Font("Inter", Font.BOLD, 18));
        sendButton.setBackground(COLOR_BUTTON);
        sendButton.setForeground(Color.WHITE);
        sendButton.setOpaque(true);
        sendButton.setBorderPainted(false);
        sendButton.setFocusPainted(false);
        sendButton.setMargin(new Insets(5, 10, 5, 10)); // Padding for the button

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // 5. Create an "ActionListener"
        ActionListener sendListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleUserInput();
            }
        };

        sendButton.addActionListener(sendListener);
        inputField.addActionListener(sendListener);

        // Initial welcome message
        appendMessage("Bot", "Hello! Type 'help' for commands or 'exit' to quit.");
    }

    /**
     * This is the bridge between the GUI and the bot's "brain"
     */
    private void handleUserInput() {
        String userInput = inputField.getText();
        if (userInput.isEmpty()) {
            return;
        }

        // Display the user's message
        appendMessage("You", userInput);

        // Get the bot's response
        String botResponse = botLogic.processInput(userInput);

        // Display the bot's response
        appendMessage("Bot", botResponse);

        // Clear the input field
        inputField.setText("");

        // Handle exit
        if (userInput.equalsIgnoreCase("exit")) {
            Timer timer = new Timer(2000, e -> System.exit(0));
            timer.setRepeats(false);
            timer.start();
        }
    }

    /**
     * A new helper method to add HTML to the chat area.
     */
    private void appendMessage(String speaker, String message) {
        String cssClass = speaker.equals("Bot") ? "bot" : "user";
        String speakerTag = speaker.equals("Bot") ? "🤖 Bot" : "🧑 You";

        // Handle newlines in the message for proper HTML rendering
        String formattedMessage = message.replace("\n", "<br>");

        htmlContent.append("<div class='message " + cssClass + "'>");
        htmlContent.append("<div class='speaker'>" + speakerTag + "</div>");
        htmlContent.append("<div>" + formattedMessage + "</div>");
        htmlContent.append("</div>");

        chatArea.setText(htmlContent.toString() + "</body></html>");
        
        // Automatically scroll to the bottom
        // We do this by setting the caret position to the end of the document
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    /**
     * The main method to start the GUI.
     */
    public static void main(String[] args) {
        // Use SwingUtilities.invokeLater for thread safety
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GuiAssistant().setVisible(true);
            }
        });
    }
}