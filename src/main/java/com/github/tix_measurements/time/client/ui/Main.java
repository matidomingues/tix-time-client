package com.github.tix_measurements.time.client.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.codec.binary.Base64;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.prefs.Preferences;

public class Main extends Application {

    // application stage is stored so that it can be shown and hidden based on system tray icon operations.
    private Stage aboutStage;
    private Stage prefStage;

    /**
     * Sets up the javafx application.
     * A tray icon is setup for the icon, but the main aboutStage remains invisible until the user
     * interacts with the tray icon.
     */
    @Override
    public void start(final Stage stage) throws Exception {

        // stores a reference to the aboutStage.
        this.aboutStage = stage;
        aboutStage.setTitle("Sobre TiX");

        // instructs the javafx system not to exit implicitly when the last application window is shut.
        Platform.setImplicitExit(false);

        // sets up the tray icon (using awt code run on the swing thread).
        javax.swing.SwingUtilities.invokeLater(this::addAppToTray);

        Parent aboutParent = FXMLLoader.load(getClass().getResource("/fxml/about.fxml"));
        Scene aboutScene = new Scene(aboutParent);
        aboutStage.setScene(aboutScene);

        prefStage = new Stage();
        prefStage.setTitle("Preferencias");

        Parent prefParent = FXMLLoader.load(getClass().getResource("/fxml/setup1.fxml"));
        Scene prefScene = new Scene(prefParent);
        prefStage.setScene(prefScene);

        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        String username = null;
        try {
            username = prefs.get("username", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (username == null) {
            prefStage.show();
            prefStage.toFront();
        }
    }

    /**
     * Sets up a system tray icon for the application.
     */
    private void addAppToTray() {
        try {
            // ensure awt toolkit is initialized.
            java.awt.Toolkit.getDefaultToolkit();

            // app requires system tray support, just exit if there is no support.
            if (!java.awt.SystemTray.isSupported()) {
                System.out.println("No system tray support, application exiting.");
                Platform.exit();
            }

            // set up a system tray icon.
            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();

            byte[] btDataFile = Base64.decodeBase64("iVBORw0KGgoAAAANSUhEUgAAALsAAACyCAYAAAAan76iAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAN1wAADdcBQiibeAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAABjiSURBVHic7Z15mB1VmcZ/LwQJkOAEg4SRIMsgiMiiyCoEMGBAlrDFKFERAQdlUGAGUAaJigi4gOKKCuqgQMK+iiyBgCyyDYMLRraEJQTZl4RsvPPHV9db3dytu+v2Xfr8nqee5N6qOvXd7rfPrTrnO+8n2xSFpB0AbN9UWKOJIYuk5YDlbM8vor1limgkxw7ZlkgUwb7A2UU1VrTYE4m2JYk9MWRIYk8MGXqKXRrbojgSiabTu2c/vyVRJBKDQFns0rrAWi2LJJFoMiF26XjgFmA40oRsG9HSyBKJgin17LcADwAGXs+24mabEok2IMRuzwTmAQuxb8q211oaWSJRMGnoMTFkGFb3CGkFYANgBPYttQ49FtZfD0YhrYP9SEExJhKF0FvsqnDMWODzwLrAjrUaOxZ2HQVvBR5GuhuYBkzHfqyAWBOJAZG/jVkArIr0lh5H2LNoMBnnGLjkLvgz8BqwOXAa8CjSnUhHpUmrRCvJi/0coqc/AGkHpBNz+95opLGfw+wt4ELg7cD+wHRgPrAF8B1gNtJtSF9EekchnyCRaJCy2O07gL2B9wNbAWf0u1V7PvaF2JMI4U8GLiaGNLcGTgceR7oV6Qikf+33tRKJBul5z25fClxa6BViCPMC4IJsomoPYBIwAdg2205HupW4x78I++lCY0gkaHzocQQwcsBXs1/FPg97b6LHnwJcDiwGtgd+ADyJNAPpMKS3D/iaiURGfbFLmxA98QtI/45UzNi8/Qr2b7D3IoT/SeBKYAmx2ulHwFNI1yMdijS6kOsmhiwqeA3qVADbUwfQyL8AexF/YDsDy2V7lgAziFudS7CfG0CoiQ5A0mRgou3JRbTXfjOo9ovYv8L+CLAa8Bng2mzvzsDPgKeRfof0aaRRrQo10Vm0n9jz2C9gn409ARgDHAJcl+39MDH+Pw/pKqRPIb21VaEm2p/2Fnse+znsn2PvAqwOfBa4gfgMuwG/BJ5BugJpCtLKrQs20Y50jtjz2M9in4U9HvhX4DDifn4YsDvwP4TwL0X6ONLAR5ISHU9nij2P/Qz2T7B3IoR/ODCTeLDdC/gNIfyLkSYjrdTCaBMtpPPFnseeh/1D7HHAGsARwK3A8sTs8HnAP5CmI+2PtGILo00MMt0l9jz2XOwzsbcjMje/CNwGDAf2I4Ywn0G6AGnfLJU50cV0r9jz2E9ifw97W+CdwFHAHcCKxHj+hYTwf4s0EWl4C6NNNImhIfY89uPYp2NvTbgp/CdwF5ES8THgEkL45yLtibR864JNFMnQE3seew72d7C3ANYGjgXuIfKADgAuI8bxf430kTfl+ic6iqEt9jz2Y9inYW9OrMr6EnAfsfLqE0Tezjykc5B2JeyUEx1EEnsl7EewT8F+H7AecDxwP/AvwIHA1UTKwi+QdkGqv5Y30XKS2OthP4R9MvamwPrACYTHzirAQUTeztNIZyGNR1q2hdEmapDE3hfsWdgnYW8MbAhMBf4CvI1y3s5cpJ8g7ZSE314ksfcX+6/YX8V+D7AR8DXgQWBVynk7TyL9EGlcYesAEv0m/QKKwP4z9onY7wY2Bk4CZhEpyp8DbiKEfybSdkiVLEsSTSaJvWjsB7BPwF4f2BQ4GXiISFEu5e08gXQG0jZJ+INHEnszse/HPh57PeB9wCnAI0TC2heAPwBzkL6LtFUSfnNJYh8s7Puwv4S9LvAB4FvAY0TC2pHA7cBjSN9G+kDrAu1ekthbgX039jHYawNbEgZSc4A1gaOBPyI9inQq0vtbGWo3kcTeauw/Yv8nkadTMpB6Int9DHA30kNI30TarGVxdgFJ7O2Cbew7sI8ievhtge8BTxHpC8cB9yLNQjoJaeMWRtuRJLG3IyH827C/SNzTlwyk5pJPX5AeRPoa0kYtjLZjSGJvd0L4t2D/ByH8HQgDqXnk0xekPyOdiPTu1gXb3iSxdxL2G9g3Y3+eGL7cCfgJ8A/y6QvSA0gnIK3fumDbjyT2TiWEPwP7MMJaZDxwFvAs+fQF6X6kLyOt18Jo24Ik9m7AXop9A/ZnCeHvAvwceJ5IX/gGMAvpXqTjkNZpYbQtI4m927CXYF+HfQiRm7MrUWjiBWAz4JuUygBJxyCt1bJYB5kk9m4mhP877IMI4X8E+BXwIlF04lTKZYCORlqzhdE2nST2oYK9GPtq7AMJ4e9BOKe9TJQB+jaRrnA70pFIa7Qu2OaQxD4UsRdhX4n9ScIbv+Sc9ipRYui7RIJaV5UBSmIf6tgLsS/HnkIIfx/gfKLiYWkW9wmkmUiHI40p5LrSboP97ZHEnihjv459CfbHCOGXnNPmA9sBZ1JEGSBpQ+Aq4L7BTHtIYk9Uxl6AfRH2Rwnhl5zTFtKzDNANSJ9FWrUPrf8duAYYDdyItGmxwVcmiT1Rnyj1OR17f2KNbanU52LKs7hzka5DOhjpbXXaW0wYzV5JLFa/cTBSmZPYE33Dfg37Aux9CeF/nHBOW0LM4ubLAB1UtQyQvRDYNzt3FHB9sxetJLEn+k+51OdE4lbnE8AVwFKiDNAvCBe1q5EOzIrD5c9fRFRCv4gwoLoOactmhZvEnigG+2Xsc7H3JMbxP0U8hJryLO68rAzQJ/5ZBihuaSYTD8JvBX6PtHUzQkxiTxSP/RL2r7F3J4T/aeKBVEQZoF8TTsmXIR0ArEDcDv0WWBm4FumDDV8v6vPWLR6XxJ5oLlHq85fYuxF2IqVSn8sCewLnAs8A04kH1vMIF+VrjodGc/P/nSgyUZP2K/qbGBrEiM0+xJDmjoT4ARYQC1PWWgKLPgd3nmVvX6WNYcQ9/2+A1bHn1bxkEnuipYT19xbAFKKo8zrE7U6eUdgvVjj3FGLia11iwms+MA373kqXSlbLicEhZlvXBzbI/i1t61Du1XvjZ2HB6MjZqbDXxyE9TTgyfKNez57Enmg+0pFEclkllhL2gA8Cf8tvitubibaXFBFGEntiMHiJuA9/hBByXtgPZ+Ptb6ZgN8Ak9kTzsc8Gzh7Ua0qHE0OaawLPYn81iT3RLZSHHqWdgQ2wD89e/wXpnjTOnuh0/i/7d0OglD48B5idO2YJsEbq2ROdjX0j0qXAj5HuIHwyj8H+FgDSOKL+1YVJ7InOx947SxdYhL3gn++HV873gAnYzyaxJ7oD+6Uer6U9gYOBvbBnIx2exJ7oPqSDCM/76cCXsgXjbySxJ7qRm4mVUHnmtJ3YJf2MSO5pJ+63Pa7oRiXNJOzpimI1xwqgatc7lihTPxCut73fANtoLvbDwMO9324rsUt6CyH0urnJg8z2kta0PafegZLWBV51nTyNjJEU+1nrTTkOL+B6IwZ4fstoK7EDH6KxX8Yi4FFi+vlhYqZsJ2DtOufNBq4nVsi/izD2H0tjef17E0/2FZE0mUhIGpO9vgs40PZfarT5YaKK3hbEAoe1GoijhIn1m/cQY83/Z/v1Oud8i/iK34ooabMH9f9AIMrXXwfcmm2dSXjdF7MR/uBTB3D+z4lfYrXtOkLUy1Q5/+wa515T5ZzlgY8Cf6pz7ZtrxL1blXPmAqMa/OzLEmmuL9WJo7QdVcDv6+g615gJfLhIjfQxvsnA+UW11zYzqJKWJWzYKnEZ8AHbO9u+0fYbVY67uMYlZlZ60/ZC2xcA7yVuoR6ocv62qu6NckKV98cQq2jqYnup7XOJspEPNnDKjY20W4fzq7y/FDjC9va2ry3gOm1B24gdGEeY5vTmONsTbd/dQBvVhArlaeWKOLiQcLf9dYVDav0xblKj6T75odieRfirP13n0E/1pd0qVFrJ/wqwp+0zC2i/rWgnse/b6/US4p731ILaX9rIQY7V7gcSds696T2cVeLJGk0+3sh1e8XweHatqiMrwMHqbU3Rd47o9Xo+MN721QNsty1pC7ErypjnhbQQ2Mv2r1oRT9bLH0eUXM+vWxyvkgVET86t0tRSYsV8f2K4AzisxiEjCJvpfiFpE+LbtMRS4KO2/9jfNtudthA7sA1RHqXEl9uhd7H9feC03FtvIR5Ge3MyYfSTZyHwedt3DeD65wA/rXHIZyQd2Nd2JQ0Hftnr7c/bvrKvbXUS7SL2/C3MTOCMVgVSga8A/5t7vU/vA2wvdky07AQcS/TI77VdS6iNcgRQq7f9kfruhPsjIG8menJBsbY17TLOXhLQq8R9erXRlkHH9iKFkc89xKTMrpKGu8KYtu0ZwIwmXH9f4F7CW7E3KwAXStrc9sv12pN0CDGmX+Iy4L8LCbbNaXnPLmkFwibteeBo24+2OKQ34ZgYOo6I8TwGeRbR9hPEXEC1h+z1CF/FmiiccvOjLH8GptgF+qm0MS0Xu+0FjiK2q9s+q9Xx1OD7wBjbh9p+ttIBklaVtJWkyZIOlFRtqLLPZN8atfJa9pP0xWo7Ja1CPFcsn731PDHEWNmmogtpl9sYXG2FeZuQ9X6LFaVRNiCMedbJ/i1tI3ud9ifiNqGoGL4laQvCGKgSp0m60/bt+TclLUO4Zr0ze2spMMn2I0XF1gm0jdg7iPOJWkOt4tPAe6jsg7gcME3SZr2+fb4CTMi9Ptr2DU2MsS1p+W1MBzIPqJdw1TSy2459iJnOSqwB/DbrzZE0gZ7pDOfYrprQ1s0ksfcRR8WJlYjV7IcR976DHcODxCxvNXYGviLpncTtS+n3fDu1J6q6miT2fmD7Ddt/tf0TokT6gnrnNCGGi+k54dWbE4AbiJX1ECkN+7jG4o5uJ4l9gDgWdJzXost/merZj8sQD80Qt117266XXNbVJLEXQ79TAgaC7aVEzne9ZLNDBpK20C0ksRdDxXH3wcD2P4h0i1q3J6vU2DdkSGIvhpfqH9JUnifKr1fj25K2Gqxg2pUk9mJo2XR7ltN+JbV779L4e6XFMUOGJPYORlFTaBoxo1ui2rfMWOA3pfH3ociQ/eBdwpnEmHqJMwjHgGoJY7tQfb1s15PE3qFI+gI9F3PPAP7L9i1EekA1viJpl6YG16YksXcgknYjvAxLzCYSu0q1h74J/L7K6csQtzNrNDHEtiSJvcOQtBGRjJavG7p3PvEry9CcQvjWVGI08cC6XDNjbTeS2DsIRXnFK+mZSnyw7ft6H5uNv3+M6vfvWxMOYUOGJPYOIVskfRnlnHSA79iu6l5g+2bgqzWa/YKkdjOR7TvS6lm165oksXcOZxMejSWuJxZ31+Mb2bHV+IWkdw0ksDbgh8Db6h2UxN4BSDqRuCUp8SgwOcuNqUm2eH0K1R3GRgIXSVpxwIG2gpg32KGRQ5PY25zMHXhq7q35xAPpc4224bDPPgCo5tqwEfDj/sbYMqSPE0sfRwE7Ik3IqmxUJIm9jZH0QeCcXm8fZPv+vrZl+0bg6zUO+WRms9FJPEg5xXkhkcpc9duu28Re6/N01GeV9G7gcvLFbOG0zHG4v3yN2u6/Z0p63wDaH1zse4GHsle3Yd9EjSIQHSWABqjl59J75X/bovgq/h3x9Vzi9wywREx2/34A8EyVQ5YHphdgmNqWdJvYa1XtqGRIWhTVJmf6PGmTecBfA6zZa9exRTilZauVqvrLEPYg52V++Z2LNAJpItLBxFrcrhN7rR5pVI19A2W1Ku+/vS+NZPWYbuPNRcUesP2/FU7pF7bPo7ZN3wRaPeEkDTsS/m0ALZwL/JWo1nIF0tbdJvbt+7lvoIyt8v4oSQ1Z5UnakhB6pV/wTf2MqxZX1Nl/pKTDm3Dd6kjLIn0I6afA3O/C194D9YZES7Ym78i1sxLwDPbfsGcTxrDbdo3YM4/33gUN8oxXlPxuxnUPqHHIlDrnj5b0Y+APVP8m+EA/w6tFrZVNJc6UdHpTb2lC4DsSP4O5xATYocDol2Heu+qLfRrxWaYgvRfpB8Dr2IcijUP6DvAy8OOiCz5NZQAFxAZw3WWJIbp6RbdmACMLvO4ywEl1rvkEsHGFc8cS7rkvNBC3iWHDZQuKex1i2K6R65rwm9mLKoXb+rzBMoYdDD8yPG1wbnvIcLLDoqSxAmKwjeHbhq8b1s69P9KwqeFKw+5ygQaukqYC2J5aWKOVr7MlUQZyVeKHMo74BTbC00R5xD8S7l7zbNeaTu997Y2Jh8fNgd1prGbSfOAS4nZkDOHjPo6+PzM9QtyL/h241pHs1UjM6wI7ErZ921G22OgrjwK3EH711zjMmhojZjo/CEwivoHH5PY+QpRen5YNJ5bingxMtD25T1HGN/jKRLkekPYATu3Inp3oaRrtleptL/bx2vVKSA7WNrEPMZ/YhOuf0kCPK8N2hu8bnurVgz9qOM2weY24+1caEg4x/CL3+suGH3Sqsem91LaO6At9tWy+i4FZZ6xHjHMP1JGgLzE8RnybFUllB+B4htmG6MH3A/LT97Mp9+DN9LE5BxiBdAxRGmgBcEJHit3h596qa3+6/lHthaMQW/OKsYXAt6Is8PwqqDnAhYTA72xaDHlixdbpvd/uSLEn2oAQ+JaUBZ4ffn2CUg8Od5buSVpNEnuib0QxhElENfD8LO+TlHpwuL1dBJ4niT1RH2lzygJfK7fnKcoCv60dBZ4niT1RmSg2VhL42rk9c4naTNOAP9BGlQ3rkcSeKCNtRgh8Ej3nLeYRPfh04JZOEnieJPahjrQp0XtPomdezjOUe/CZnSrwPEnsQ5GYBS714Ovl9vwDuJgQ+M00sMa1k0hiHyqEuVJJ4Ovn9jxLWeA3dZvA8ySxdzPSeyg/ZOZLST5H5OpMA2ZQts3rapLYu41Yu1rqwTfM7XmessBvHCoCz5PE3g1IG1B+yNwot+cF4FJC4DdgL25BdG1DEnunEi5epR78vbk9LxICnw5cN9QFnieJvZOQ/o2ywDfJ7XmJ8IGcRgh8UQuia3uS2NudWHhResjcLLfnZcJXZhpwbRJ4fZLY2xFpHcr34HnTolfoKfAhW626PySxtwvSWpQFvnluz6uEE8A04HfYr7/p3ERDJLG3kjDvKQk87yDwGmWBX5MEXgxJ7IONNJaywLfM7XkNuIoQ+NXYC1oQXVeTxD4YRLGu/QiBbwUo2zOfEPh04Crs+a0JcGiQxN4spHdQFvjWlAW+ALia6MGvwm7ErChRAEnsRSKtTlng21IW+OuEWek04Iok8NaQxD5QpDGEwPcnTIBKxkevE7bTJYH31bIjUTBJ7P1BWo1wtZpEOGyVBL4QuJYQ+OXYr7QmwEQlktgbJWqQ7kMIPG9dt4ieAn+5NQEm6pHEXosoDJAXeMnNdhHh+z0NuAx7oO5eiUEgib030mhgb0LgO1IW+GKiB58OXIr9YmsCTPSXJHYAaRXKAt+J8s9lMeUe/FLsF1oTYKIIhq7YpVGUBf4hyj+LJZR78Euwn29NgImiGVpijypwEwmBj6dc4GsJ5R78EvpQUDfROXS/2MOYfiIxDr4zYWEMURz2BkLgF2MPxIY60QF0p9illYmyKJOAXegp8BspC7yhyhWJ7qB7xC6NBPYkBP5hooAtwBtEeZdpwEXY1QreJrqcwRd7VF5bDfupAtoaCexBCHwCPQV+M2WBVy3xnRg6DK7YpbcQBpl7II3DntmPNkYQhbsmAbsCw7M9bxDFrUoCn1tIzImuYfDELi1PmPTsShj2VK7JU/nclYCPEALfDVgh22PgVmKY8MJCvi0SXcvgiF1agfAy2YXwFhyP/USdc1akp8BLxV9NVIKeRgj8ySZFnegymi/2EO3lxMTNM8CHsP9U5dgVCGFPIm5V8gK/gxD49Lp/KIlEBZor9rj9uBLYgSi2uxP2X3sdM5wQ+P7Ew+ZKub13Uu7B5zQ11kTX0zyxx4Pk1US+95OE0Gdl+4YToyeTCIGPyJ15F+UefHbT4ksMOZoj9pjUuYYo/vo4kT34BFJpHHxPYGTujLspC/yxpsSUGPKUxS6NpVTrfQCsEWPdvydsImYDJxFl3vcEVs4dei9lgTc+MpNI9JN8z34+sUi432wJb70cpgCjCau2VYCf5Q65j3I574cHcq1Eoq+E2KUNgTEDbexWOHxY+Q+odJvyLDGSchEwA5jT7vUyE93JMKSpwGHAG0gTsvdn9sew52V4ZRUYRQwVlmwkRhPDiLtnrxcgzQL+lm0PZv8+kJxoE81kGJEkNY4oKlXyFOxXz/s2+P56sOIsOI4o9b1+tm2Q+/87CG/xTXqdfgEwuT/XTSQaYRj2TUifA9bEvmmgDf4d5mc1Mx/Ltmt7HBBDkr3/CNYm8loSiaYx+FmPYRZ0T7YlEoPGMvUPAaRtsho+iUTHkhe7Kh4RpQYvILIVE4mOpST2BcAYpJ49vTQMOJQYg08kOpqSuM8hcsQnI41HOj57/yjgdGLtZiLR0cQDaozI7EcYBM0Gvoe0O/A37Dmo8h1OItFJlEdj7IuIWc5A+hTwHNKuRJ7LfKQ1sP9rsINMJIqg+tCjvf8//y+dAczFPnUQYkokmkL9oUfp/cBYYGOkDZoeUSLRJOpPKtn3EMb7iURH09ikUiLRBRSdLpCK0ybalkLFbvuUIttLDHn+RDgsF8L/A3UONgWdcyQdAAAAAElFTkSuQmCC");
            java.awt.Image image = ImageIO.read(new ByteArrayInputStream(btDataFile));
            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(image);

            // if the user double-clicks on the tray icon, show the main app aboutStage.
            trayIcon.addActionListener(event -> Platform.runLater(this::showAboutStage));

            // if the user selects the default menu item (which includes the app name),
            // show the main app aboutStage.
            java.awt.MenuItem openItem = new java.awt.MenuItem("Sobre TiX");
            openItem.addActionListener(event -> Platform.runLater(this::showAboutStage));

            java.awt.MenuItem openPrefs = new java.awt.MenuItem("Preferencias");
            openPrefs.addActionListener(event -> Platform.runLater(this::showPrefStage));

            // to really exit the application, the user must go to the system tray icon
            // and select the exit option, this will shutdown JavaFX and remove the
            // tray icon (removing the tray icon will also shut down AWT).
            java.awt.MenuItem exitItem = new java.awt.MenuItem("Salir");
            exitItem.addActionListener(event -> {
                Platform.exit();
                tray.remove(trayIcon);
            });

            // setup the popup menu for the application.
            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(openPrefs);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

            // add the application tray icon to the system tray.
            tray.add(trayIcon);
        } catch (java.awt.AWTException | IOException e) {
            System.out.println("Unable to init system tray");
            e.printStackTrace();
        }
    }

    /**
     * Shows the application aboutStage and ensures that it is brought ot the front of all stages.
     */
    private void showAboutStage() {
        if (aboutStage != null) {
            aboutStage.show();
            aboutStage.toFront();
        }
    }

    private void showPrefStage() {
        if (prefStage != null) {
            prefStage.show();
            prefStage.toFront();
        }
    }

    public static void main(String[] args) throws IOException, java.awt.AWTException {
        Main.launch(args);
    }
}