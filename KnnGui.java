package knngui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class KnnGui {
	public static File[] selectedBinaryFiles;
	public static File selectedImg;
	public static ArrayList<Integer> tempList = new ArrayList<>();
	public static Thread algoThread;
	public static DecimalFormat decimalFormat = new DecimalFormat("####0.00");


	public static void main(String[] args) {
		//Variable
		JFrame main_window = new JFrame(); // Main frame
		          
		//GUi Components
		JLabel k_header, k_label, input_label, test_label, prg_label, img_label, result_label;
		JTextField k_textField;
		JButton browse_input_btn, browse_test_btn, start_btn, stop_btn; 

		//Initializing GUI components
		JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView());
		fileChooser.setMultiSelectionEnabled(true);
		JFileChooser dirChooser = new JFileChooser(FileSystemView.getFileSystemView());
		dirChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		JProgressBar prgBar = new JProgressBar();
		
		k_header = new JLabel("KNN Classifier");
		k_label = new JLabel("Enter K value (0-9):");
		k_textField = new JTextField();
		input_label = new JLabel("Select training data:");
		test_label = new JLabel("Select test image:");
		browse_input_btn = new JButton("Browse");
		browse_test_btn = new JButton("Browse");
		start_btn = new JButton("Start");
		stop_btn = new JButton("Stop");
		prg_label = new JLabel("Progress", SwingConstants.CENTER);
		img_label = new JLabel();
		result_label = new JLabel();

		
		//Placing all the components
		k_header.setBounds(200, 10, 120, 20);
		k_label.setBounds(20, 50, 150, 20);
		k_textField.setBounds(200, 50, 120, 20);
		input_label.setBounds(20, 80, 150, 20);
		browse_input_btn.setBounds(200, 80, 120, 20);
		test_label.setBounds(20, 110, 150, 20);
		browse_test_btn.setBounds(200, 110, 120, 20);

		start_btn.setBounds(200, 150, 120, 20);
		prg_label.setBounds(200, 200, 120, 20);
		prgBar.setBounds(20, 240, 460, 30);
		stop_btn.setBounds(200, 280, 120, 20);
		result_label.setBounds(20, 320, 300, 20);
		img_label.setBounds(300, 320, 32,32);

		stop_btn.setVisible(false);
		prgBar.setStringPainted(true);

		//Triggers for all buttons
		browse_input_btn.addActionListener(e -> {
			int returnValue = fileChooser.showSaveDialog(null);
			if (returnValue == JFileChooser.APPROVE_OPTION){
				selectedBinaryFiles = fileChooser.getSelectedFiles();
			}
		});
		browse_test_btn.addActionListener(e -> {
			int returnValue = dirChooser.showSaveDialog(null);
			if (returnValue == JFileChooser.APPROVE_OPTION){
				selectedImg = dirChooser.getSelectedFile();
			}
		});
		//Trigger for start button
		start_btn.addActionListener(e -> {
			//Checking if K value is given or not and if Given is in between 0-9
			if (k_textField.getText() == null || Integer.parseInt(k_textField.getText()) < 0 ||Integer.parseInt(k_textField.getText()) > 10){
				JOptionPane.showMessageDialog(new JFrame(), "Please Enter key value", "Dialog",
						JOptionPane.ERROR_MESSAGE);
			}else {
				algoThread = new Thread(() -> runThread(prgBar, result_label, img_label, Integer.valueOf(k_textField.getText())));
				stop_btn.setVisible(true);
				prgBar.setValue(0);

				result_label.setVisible(false);
				img_label.setVisible(false);

				algoThread.start();
			}
		});
		stop_btn.addActionListener(e -> {
			result_label.setVisible(false);
			img_label.setVisible(false);
			algoThread.stop();
		});
		
		//Adding all components to main_frame
		main_window.add(k_header);
		main_window.add(k_label);
		main_window.add(k_textField);
		main_window.add(input_label);
		main_window.add(browse_input_btn);
		main_window.add(test_label);
		main_window.add(browse_test_btn);
		main_window.add(start_btn);
		main_window.add(stop_btn);
		main_window.add(prg_label);
		main_window.add(prgBar);
		main_window.add(result_label);
		main_window.add(img_label);

		//main configuration
		main_window.setSize(500,500);
		main_window.setResizable(false);
		main_window.setLayout(null); 
		main_window.setVisible(true);
	}
	private static void runThread(JProgressBar prg, JLabel re_label, JLabel img_label, Integer k){
		//Initializing local variables
		boolean isSingleImage = false; //stores if test directory is selected or one file
		ArrayList<Integer> resultList = new ArrayList<>(); //list to store each image result
		ArrayList<Integer> img_index = new ArrayList<>(); //to keep track of original image
		ArrayList<File> test_images = new ArrayList<>(); //to store all test images
		BufferedImage image = null; //buffer to read image file

		//Checking if selected image is directory or single file....adding them to test_images
		if (selectedImg.isDirectory()){
			test_images.addAll(Arrays.asList(Objects.requireNonNull(selectedImg.listFiles())));
		}else {
			test_images.add(selectedImg);
			isSingleImage = true;
		}
		//initial progress is 0
		int prgValue = 0;
		//maximum progress is = total number test of images * total training images
		int maxPrg = test_images.size() * selectedBinaryFiles.length * 10000; //as each file consist 10000 train images
		int label = 0;

		prg.setMaximum(maxPrg);
		//Iterating through each image
		for (File test_image : test_images) {
			TreeMap<Double, Integer> sortedTree = new TreeMap<>(); //tree map to store result of each train image vs test image
			try {
				String img_name = test_image.getName(); // getting name of the test image file
				img_index.add(Integer.valueOf(img_name.split("_")[0])); // getting original label of the test image
				image = new BufferedImage(32, 32, //buffering input test image
						BufferedImage.TYPE_INT_RGB); //RGB channel

				image = ImageIO.read(test_image); // reading that image

				byte[] img = ((DataBufferByte) image.getData().getDataBuffer()).getData(); //converting values to flatten array
				ArrayList<Integer> t_binary = new ArrayList<>();
				// writing each byte to arraylist to compare easily
				for (byte b : img) {
					t_binary.add((int) b);
				}
				// Iterating over training data
				for (File train_file : selectedBinaryFiles) {
					try {
						//open binary file
						FileInputStream fis = new FileInputStream(train_file);
						//Counter to keep track of bytes
						int counter = 0;
						int ch;
						while ((ch = fis.read()) != -1) {
							//if counter is 0 then it will be label
							if (counter == 0) {
								label = ch;
								counter += 1;
							} else if (counter == 3072) { // if it is 3072 then it will be end of that image
								counter = 0;
								prgValue += 1; // increase progress
								prg.setValue(prgValue); // set progress
								sortedTree.put(e_dist(tempList, t_binary), label); // put result to treeMap
								tempList = new ArrayList<>(); //create new list to store new train image

							} else {
								tempList.add(ch); // adding bytes to temp list
								counter += 1;
							}
						}

						fis.close(); // close file

					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			//Showing final results
			//If it is single image then show label and accuracy along with the image
			if (isSingleImage) {
				re_label.setVisible(true);
				img_label.setVisible(true);

				ArrayList<Integer> k_list = new ArrayList<>(); // k neighbour values list
				ArrayList<Double> key_values = new ArrayList<>(sortedTree.keySet()); // ecludian distance difference
				for (int m = 0; m+1 < k; m++) {
					k_list.add(sortedTree.get(key_values.get(m))); // create k value list
				}
				int f_label = mostCommon(k_list); // get the most common label from that
				// set final text field
				// accuracy calculating inline
				re_label.setText("Label: " + f_label + "  with confidence: " + decimalFormat.format(100 - ((key_values.get(0) / totalDist(key_values))*1000000)));
				assert image != null;
				//showing image
				ImageIcon icon = new ImageIcon(image);
				img_label.setIcon(icon);
			} else {
				//if not single image then add result to result list
				ArrayList<Integer> k_list = new ArrayList<>();
				ArrayList<Double> key_values = new ArrayList<>(sortedTree.keySet());
				for (int m = 0; m+1 < k; m++) {
					k_list.add(sortedTree.get(key_values.get(m)));
				}
				int f_label = mostCommon(k_list);
				resultList.add(f_label);
			}
		}

		if (!isSingleImage){
			//as if its not single image......calculating average accuracy
			img_label.setVisible(false);
			re_label.setVisible(true);
			// count for accurate result images
			int accurateResult = 0;
			for (int i =0; i < img_index.size(); i++){
				if (resultList.get(i).equals(img_index.get(i))){
					accurateResult += 1; // if output by algorithm and label by original image is equal increase accurate count
				}
			}

			double accuracy = (((float)accurateResult/resultList.size()) * 100);//converting accurate image count to percentage accuracy
			re_label.setText("Current accuracy with k = " + k + " is : " + decimalFormat.format(accuracy) + "%");//displaying accuracy
		}

	}
	//function to find most common result from k_list
	public static <T> T mostCommon(List<T> list) {
		Map<T, Integer> map = new HashMap<>();

		for (T t : list) {
			Integer val = map.get(t);
			map.put(t, val == null ? 1 : val + 1);
		}

		Map.Entry<T, Integer> max = null;

		for (Map.Entry<T, Integer> e : map.entrySet()) {
			if (max == null || e.getValue() > max.getValue())
				max = e;
		}

		assert max != null;
		return max.getKey();
	}
	// function to calculate avrg total dist for entire image set
	private static double totalDist(ArrayList<Double> inList){
		double r_value = 0.0;
		for (double d : inList){
			r_value += d;
		}
		return r_value;
	}
	//euclidean distance function
	private static double e_dist(ArrayList<Integer>img_train, ArrayList<Integer> img_test) {
		double dist = 0.0; // initial distance 0
		for (int i = 0; i < img_train.size(); i++) { // for 3072 bytes
			dist += ((img_train.get(i) - img_test.get(i)) * (img_train.get(i) - img_test.get(i)));// euclidean formula
		}
		//returning final distance's square root
		return Math.sqrt(dist);
	}

}
