package taraprasad73.wordattack;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This fragment handles chat related UI which includes a list view for messages
 * and a message entry field with send button.
 */
public class WiFiChatFragment extends Fragment {
    private Set<String> allWords;
    private List<String> mLines;
    private final long maxTime = 20000;
    public int turn;
    ChatMessageAdapter adapter = null;
    private ChatManager chatManager;
    private TextView chatLine;
    private ListView listView;
    private Button sendButton;
    private TextView myScoreText;
    private int myScore;
    private int opponentScore;
    private TextView opponentScoreText;
    private CountDownTimer countDownTimer;
    private TextView countDownTimerText;
    private List<String> items = new ArrayList<String>();
    private int countDownTimeMe;
    private int countDownTimeOpponent;
    private String lastOpponentWord;
    private int totalWords;
    private int maxWords = 10;
    private boolean opponentLost;
    private boolean gameEnded = false;
    private boolean sendRestartRequest;

    private boolean checkWord(String word) {
        if(allWords.contains(word)) {
            return true;
        } else {
            return false;
        }
    }
    private void importFile() {
        mLines = new ArrayList<>();
        //AssetManager am = context.getAssets();
        try {
            BufferedReader reader;
            InputStream is = getActivity().getAssets().open("words.rtf");
            //InputStream is = context.getResources().openRawResource(R.raw.words);
            //InputStream is = am.open(path);
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                mLines.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        allWords = new HashSet<String>(mLines);
    }
    public int getMaxWords() {
        return maxWords;
    }

    public int getTotalWords() {
        return totalWords;
    }

    public void setTotalWords(int totalWords) {
        this.totalWords = totalWords;
    }

    public void setLastOpponentWord(String lastOpponentWord) {
        this.lastOpponentWord = lastOpponentWord;
    }

    public void endNormallyResult() {
        if (myScore > opponentScore) {
            countDownTimerText.setText(R.string.result_win);
        } else if (opponentScore > myScore) {
            countDownTimerText.setText(R.string.result_loss);
        } else {
            countDownTimerText.setText(R.string.result_tie);
        }
        chatLine.setVisibility(View.GONE);
        sendButton.setText("Rematch");
        gameEnded = true;
        //listView.setEnabled(true);

        try {
            countDownTimer.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void endOnTimeResult() {
        if (opponentLost) {
            countDownTimerText.setText("Your Opponent Lost On Time");
        } else {
            countDownTimerText.setText("You Lost On Time");
        }
        chatLine.setVisibility(View.GONE);
        sendButton.setText("Rematch");
        gameEnded = true;
        //listView.setEnabled(true);
    }

    public void setCountDownTimeOpponent(int countDownTimeOpponent) {
        this.countDownTimeOpponent = countDownTimeOpponent;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        importFile();
        chatLine = (TextView) view.findViewById(R.id.txtChatLine);
        sendButton = (Button) view.findViewById(R.id.sendButton);
        myScoreText = (TextView) view.findViewById(R.id.myScoreText);
        opponentScoreText = (TextView) view.findViewById(R.id.opponentScoreText);
        showInstructions();
        countDownTimerText = (TextView) view.findViewById(R.id.countDownTimerText);
        countDownTimer = new CountDownTimer(maxTime, 1000) {

            public void onTick(long millisUntilFinished) {
                countDownTimeMe = (int) millisUntilFinished / 1000;
                String text = "Time Left: " + countDownTimeMe;
                countDownTimerText.setText(text);
            }

            public void onFinish() {
                chatManager.write("#finished#".getBytes());
                endOnTimeResult();
            }
        };
        listView = (ListView) view.findViewById(android.R.id.list);
        //Implement a custom text view
        adapter = new ChatMessageAdapter(getActivity(), android.R.id.text1,
                items);
        listView.setAdapter(adapter);
        listView.setEnabled(false);

        sendButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        if (chatManager != null) {
                            if (!gameEnded) {
                                if (chatLine.getText().toString() != null) {//Preventing null pointer exception
                                    String word = chatLine.getText().toString();
                                    if (word.isEmpty() == false) {
                                        if (filterPassed(word.toLowerCase())) {
                                            countDownTimer.cancel();
                                            //sends the string entered to the other device through the chat Managers
                                            //write() method which sends the bytes to the socket's output stream
                                            String count = String.valueOf(countDownTimeMe);
                                            String toSend = word.toLowerCase() + "|" + count + "|";
                                            chatManager.write(toSend.getBytes());
                                            updateMyScore(word.toLowerCase());
                                            totalWords++;
                                            pushMessage("Me|" + chatLine.getText().toString() + "|");//adds the string into the chat fragment
                                            //by adding the "Me:" tag to it
                                            chatLine.setText("");//Clear the chatLine
                                            chatLine.clearFocus();//Get the cursor out of the editText chatLine
                                            updateSendButton(false);
                                            countDownTimerText.setText("");
                                            if (totalWords == maxWords) {
                                                endNormallyResult();
                                            }
                                        }
                                    } else {
                                        showFilterToast(0);//Empty string toast
                                    }
                                }
                            } else {
                                sendRestartRequest = true;
                            }
                        }
                    }
                });
        return view;
    }

    private boolean filterPassed(String word) {
        boolean filterResult = false;
        boolean isAlphabetical;
        boolean repeated = false;
        boolean validWord = false;
        boolean lastLetterMatch = true;//For allowing the first word of the match
        if (isAlpha(word)) {
            isAlphabetical = true;
        } else {
            isAlphabetical = false;
            showFilterToast(2);
        }
        if ((lastOpponentWord != null) && !lastOpponentWord.isEmpty()) {//For the first word of the match
            if (word.charAt(0) == lastOpponentWord.charAt(lastOpponentWord.length() - 1)) {
                lastLetterMatch = true;
            } else {
                lastLetterMatch = false;
                showFilterToast(1);
            }
        }
        if (isAlphabetical && lastLetterMatch) {

            try {
                if (checkWord(word))
                   validWord = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (validWord == false) {
                showFilterToast(3);
            }
        }

        if (validWord) {
            try {
                if(adapter.messages.contains(word)) {
                    repeated = true;
                    showFilterToast(4);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(validWord == true && repeated == false){
            filterResult = true;
        }
        return filterResult;
    }

    private void showFilterToast(int errorCode) {
        if (errorCode == 0) {
            Toast toast = Toast.makeText(getActivity(), "Nothing to send", Toast.LENGTH_SHORT);
            LinearLayout toastLayout = (LinearLayout) toast.getView();
            TextView toastTV = (TextView) toastLayout.getChildAt(0);
            toastTV.setTextSize(25);
            toast.show();
        } else if (errorCode == 1) {
            if (lastOpponentWord != null && !lastOpponentWord.isEmpty()) {
                char lastLetter = lastOpponentWord.charAt(lastOpponentWord.length() - 1);
                Toast toast = Toast.makeText(getActivity(), "Give a word starting with \"" + lastLetter + "\".", Toast.LENGTH_SHORT);
                LinearLayout toastLayout = (LinearLayout) toast.getView();
                TextView toastTV = (TextView) toastLayout.getChildAt(0);
                toastTV.setTextSize(25);
                toast.show();
            }
        } else if (errorCode == 2) {
            Toast toast = Toast.makeText(getActivity(), "Invalid word: Contains non-alphabetical characters", Toast.LENGTH_SHORT);
            LinearLayout toastLayout = (LinearLayout) toast.getView();
            TextView toastTV = (TextView) toastLayout.getChildAt(0);
            toastTV.setTextSize(25);
            toast.show();
        } else if (errorCode == 3) {
            Toast toast = Toast.makeText(getActivity(), "Not a Dictionary Word", Toast.LENGTH_SHORT);
            LinearLayout toastLayout = (LinearLayout) toast.getView();
            TextView toastTV = (TextView) toastLayout.getChildAt(0);
            toastTV.setTextSize(25);
            toast.show();
        } else if(errorCode == 4) {
            Toast toast = Toast.makeText(getActivity(), "Word already used", Toast.LENGTH_SHORT);
            LinearLayout toastLayout = (LinearLayout) toast.getView();
            TextView toastTV = (TextView) toastLayout.getChildAt(0);
            toastTV.setTextSize(25);
            toast.show();
        }
    }

    private boolean isAlpha(String name) {

        return name.matches("[a-zA-Z]+");
    }

    public void updateSendButton(boolean state) {
        sendButton.setClickable(state);
        //task - try to get the keyboard up
        chatLine.requestFocus();
    }

    private void showInstructions() {
        Toast toast = Toast.makeText(getActivity(), "You get 5 words each which will decide your result," +
                " the first word does not count towards score. " +
                "You lose if failed to place your word within the alloted time. " +
                "Your score is the time left added to twice the length of your word.", Toast.LENGTH_LONG);
        LinearLayout toastLayout = (LinearLayout) toast.getView();
        TextView toastTV = (TextView) toastLayout.getChildAt(0);
        toastTV.setTextSize(25);
        toast.show();
    }

    //Initialization of chat Fragment after the UI Thread receives the message from Chat Fragment itself which itself is created
    //by either the client or the group owner
    public void startCountDownTimer() {
        countDownTimer.start();
    }

    public void setChatManager(ChatManager obj) {
        chatManager = obj;
    }

    private void updateMyScore(String word) {
        if (totalWords == 0 || totalWords == 1) {
            myScoreText.setText("Me: " + myScore);
        } else {
            myScore += (countDownTimeMe + word.length() * 2);
            myScoreText.setText("Me: " + myScore);
        }
    }

    public void updateOpponentScore(String word) {
        if (totalWords == 0 || totalWords == 1) {
            opponentScoreText.setText("Buddy: " + opponentScore);
        } else {
            opponentScore += (countDownTimeOpponent + word.length() * 2);
            opponentScoreText.setText("Buddy: " + opponentScore);
        }
    }

    public void pushMessage(String readMessage) {
        adapter.add(readMessage);//adds the string message to the array list
        adapter.notifyDataSetChanged();//most probably calls getView method of the
        //adapter for embedding the string message into a TextView
    }

    public void setOpponentLost(boolean opponentLost) {
        this.opponentLost = opponentLost;
    }

    public interface MessageTarget {
        Handler getHandler();
    }

    /**
     * ArrayAdapter to manage chat messages.
     */
    public class ChatMessageAdapter extends ArrayAdapter<String> {

        List<String> messages = null;

        public ChatMessageAdapter(Context context, int textViewResourceId,
                                  List<String> items) {
            super(context, textViewResourceId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (turn % 2 == 0) {
                    v = vi.inflate(R.layout.custom_list_item1, null);
                    turn++;
                } else {
                    v = vi.inflate(R.layout.custom_list_item2, null);
                    turn++;
                }
            }
            String message = items.get(position);
            if (message != null && !message.isEmpty()) {
                TextView nameText;
                nameText = (TextView) v
                        .findViewById(R.id.text1);
                //android.R.id.text1 is the id
                // of the TextView defined in the android's predefined layout android.layout.simple_list_item1.


                if (nameText != null) {

                    if (message.startsWith("Me|")) {//Apply styles, according to the name of the message creator
                        nameText.setTextAppearance(getActivity(),
                                R.style.myText);
                    } else {//message starts with "Buddy|"
                        nameText.setTextAppearance(getActivity(),
                                R.style.opponentText);
                    }

                }
                //using string tokenizer
                String messageWord;
                String person;
                StringTokenizer st = new StringTokenizer(message, "|");
                person = st.nextToken();
                messageWord = st.nextToken();
                nameText.setText(messageWord.toLowerCase());
                messages.add(messageWord);
                //Adds the text in String format to the textView of the layout
                //Its possible to remove Me and Buddy from the TextView, use the nameText.setText(message)
                //inside the if statements, after removing the Me and Buddy phrases from them.
            }
            return v;
        }
    }
}
